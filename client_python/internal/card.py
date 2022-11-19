import hashlib
import re
from dataclasses import dataclass
from pathlib import Path
from typing import TypeVar, Generic, Callable, Tuple

import rsa
from smartcard.Exceptions import NoCardException
from smartcard.System import readers
from smartcard.util import toHexString

from .consts import *

T = TypeVar('T')
U = TypeVar('U')

DESCRIPTION_COLOR = {
    "E": RED,  # Error
    "W": YELLOW,  # Warning
    "I": GREEN,  # Info
    "S": MAGENTA,  # Security
}


class Logger:
    log_verbose = False


def log(*msg):
    if Logger.log_verbose:
        print(YELLOW("-"), *msg)


def apply_color(msg, kind):
    return DESCRIPTION_COLOR[kind](msg)


with open(Path(__file__).with_name("response_descriptions.txt")) as fp:
    raw_desc = [line.strip().split("\t") for line in fp]
    response_descriptions = [(
        sw1,
        # convert .. blocks to regex group so we can extract values later
        re.sub(r"^\.", "(.", re.sub(r"\.$", ".)", sw2)),
        # make the message colored depending on the kind
        apply_color(desc, kind)
    ) for sw1, sw2, kind, desc in raw_desc]


@dataclass
class StatusWord:
    sw1: int
    sw2: int

    @property
    def sw(self):
        return self.sw1 << 8 | self.sw2

    @property
    def is_success(self):
        return self.sw == SW_COMMAND_SUCCESS

    def get_description(self):
        for s1, s2, desc in response_descriptions:
            if re.match(s1, "%02X" % self.sw1) and (m2 := re.match(s2, "%02X" % self.sw2)):
                if "." in s2:
                    return re.sub(r"\\x+\\", str(int(m2.group(1), 16)), desc, flags=re.IGNORECASE)
                return desc
        return "Unknown response"

    def __str__(self):
        return MAGENTA(f"{self.sw1:02X} {self.sw2:02X}") + f" ({self.get_description()})"


@dataclass
class Response(Generic[T], Exception):
    data: bytes
    status: StatusWord

    def __post_init__(self):
        if T == bytes:
            self._processed = self.data

    def with_processor(self, processor: Callable[["Response[T]"], U]):
        res = Response[U](self.data, self.status)
        res._processed = processor(self)
        return res

    @property
    def processed(self):
        if not hasattr(self, "_processed"):
            self._processed = self._processor(self) if hasattr(
                self, "_processor") else self.data
        return self._processed

    def __str__(self) -> str:
        return str(self.status)


def command(auth=False):
    def decorator(func):
        func.is_command = True
        func.requires_auth = auth
        return func

    return decorator


class Card:
    def __init__(self, connection):
        self.connection = connection

    def query(self, *command):
        data, sw1, sw2 = self.connection.transmit(list(command))
        sw = StatusWord(sw1, sw2)
        log(GREEN("->"),
            " ".join("%02X" % c for c in command))
        log(BLUE("<-"), sw)
        data = bytes(data)
        if data:
            log(RED("<-"),
                f"{YELLOW(len(data))} bytes read:",
                "[" + " ".join("%02X" % c for c in data) + "]",
                "==",
                data.decode("utf-8")  # display result as string
                # check if result only contains printable characters
                if all(32 <= c <= 127 for c in data)
                else "<binary>")  # if not then it's probably a blob
        return Response(data, sw)

    def instr(self, ins, p1=0, p2=0, write=bytes(), recv=0, cla=CLA_PROJET) -> Response[bytes]:
        write = bytes(write)
        instr_name = get_instruction_name(ins)
        if instr_name:
            instr_name = instr_name[0][4:]
        else:
            instr_name = f"{ins:02X}"
        log(
            f"{GREEN('Instruction')} {YELLOW(instr_name)} with "
            f"P1={YELLOW(f'{p1:02X}')} "
            f"P2={YELLOW(f'{p2:02X}')} "
            f"write=[{YELLOW(toHexString(list(write)))}] "
            f"read={YELLOW(f'{recv}')}")
        res = self.query(cla, ins,
                         p1, p2,
                         # send length and data only if `write` is not empty
                         *([len(write), *write] if write else []),
                         recv)
        if res.status.sw1 == 0x6C:
            log(YELLOW("Retrying with length=SW2"))
            return self.instr(ins, p1, p2, write, res.status.sw2)
        elif res.status.sw1 == 0x61:  # GET RESPONSE
            log(YELLOW("Retrying with GET RESPONSE"))
            return self.instr(cla=0xA0, ins=0xC0, p1=0, p2=0, recv=res.status.sw2)
        if not res.status.is_success:
            raise res
        return res

    @command()
    def hello(self) -> Response[str]:
        """Get a greeting from the card"""
        return self.instr(0x01).with_processor(lambda res: res.data.decode("utf-8"))

    @command()
    def login(self, pin):
        """Login with the given PIN"""
        self.instr(0x02, write=bytes(map(int, pin)))

    @command(auth=True)
    def change_pin(self, new_pin):
        """Change the PIN to new_pin"""
        self.instr(0x03, write=bytes(map(int, new_pin)))

    @command(auth=True)
    def logout(self):
        """Log out from the card"""
        self.instr(0x04)

    @command()
    def factory_reset(self):
        """Reset the card to its factory state"""
        self.instr(0x05)
        print("Note: you will need to re-login after this")

    @command(auth=True)
    def sign(self, data) -> Response[bytes]:
        """Sign the given data using SHA-256"""
        if type(data) == str:
            data = data.encode("utf-8")
        digest = hashlib.sha256(data).digest()
        return self.instr(0x06, write=digest)

    @staticmethod
    def _deserialize_pair(data: bytes) -> Tuple[int, int]:
        """Deserialize a data block with the structure { len1: u16, data1: u8[len1], len2: u16, data2: u8[len2] }"""
        len_1 = int.from_bytes(data[:2], "big")
        num_1 = int.from_bytes(data[2:2 + len_1], "big")
        len_2 = int.from_bytes(data[2 + len_1:2 + len_1 + 2], "big")
        num_2 = int.from_bytes(data[2 + len_1 + 2:2 + len_1 + 2 + len_2], "big")
        return num_1, num_2

    @command()
    def get_public_key(self) -> Response[rsa.PublicKey]:
        """Get the public key of the card"""
        return self.instr(0x07).with_processor(lambda res: rsa.PublicKey(*Card._deserialize_pair(res.data)[::-1]))

    @command(auth=True)
    def get_private_key(self) -> Response[Tuple[int, int]]:
        """Get the private key of the card"""
        return self.instr(0x08).with_processor(lambda res: Card._deserialize_pair(res.data))

    @command(auth=True)
    def export_keypair(self, outfile):
        """Export the keypair of the card to outfile.pem and outfile.pub"""
        p, q = self.get_private_key().processed
        pubkey = self.get_public_key().processed
        # reconstruct d value
        d = pow(pubkey.e, -1, (p - 1) * (q - 1))
        privkey = rsa.PrivateKey(pubkey.n, pubkey.e, d, p, q)
        with open(outfile + ".pem", "wb") as fp:
            fp.write(rsa.PrivateKey.save_pkcs1(privkey, "PEM"))
        with open(outfile + ".pub", "wb") as fp:
            fp.write(rsa.PublicKey.save_pkcs1(pubkey, "PEM"))

    @command()
    def verify(self, infile: str):
        """Verify the signature infile.sign for the file infile"""
        with open(infile, "rb") as fp:
            data = fp.read()
        with open(infile + ".sign", "rb") as fp:
            signature = fp.read()
        pubkey = self.get_public_key().processed
        ok = rsa.verify(data, signature, pubkey)
        return "Signature is " + (GREEN("valid") + f" (using {MAGENTA(ok)})" if ok else RED("invalid"))

    def commands(self):
        members = {name: getattr(self, name) for name in dir(self)}
        return {name: func for name, func in members.items() if hasattr(func, "is_command")}


def init_card() -> Card:
    for reader in readers():
        try:
            connection = reader.createConnection()
            connection.connect()
            print(reader)
            print("ATR:", YELLOW(toHexString(connection.getATR())))
            card = Card(connection)
            log(CYAN("Creating applet command"))
            card.instr(cla=0x80, ins=0xE8, p1=0x00, p2=0x00, write=APPLET_AID)
            log()
            log(CYAN("Selecting applet"))
            card.instr(cla=0x00, ins=0xA4, p1=0x04, p2=0x00, write=APPLET_AID)
            log()
            return card
        except NoCardException:
            print(reader, "No card inserted")
            exit()
    else:
        print("No reader found")
        exit()
