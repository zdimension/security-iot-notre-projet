from dataclasses import dataclass
from smartcard.Exceptions import NoCardException
from smartcard.System import readers
from smartcard.util import toHexString
import re
from colorama import Fore, Style

DESCRIPTION_COLOR = {
    "E": Fore.RED, # Error
    "W": Fore.YELLOW, # Warning
    "I": Fore.GREEN, # Info
    "S": Fore.MAGENTA, # Security
}

def apply_color(msg, kind):
    return DESCRIPTION_COLOR[kind] + msg + Style.RESET_ALL


with open("response_descriptions.txt") as fp:
    raw_desc = [line.strip().split("\t") for line in fp]
    response_descriptions = [(sw1, re.sub(r"^\.", "(.", re.sub(r"\.$", ".)", sw2)), apply_color(desc, kind)) for sw1, sw2, kind, desc in raw_desc]

CLA_PROJET = 0x42
APPLET_AID = [0xA0, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x10, 0x01]


def instr(ins, p1=0, p2=0, write=[], recv=0, cla=CLA_PROJET):
    res = query(cla, ins, p1, p2, *
                ([len(write), *write] if write else []), recv)
    if res.sw1 == 0x6C:
        print(f"{Fore.YELLOW}*** Retrying with correct length{Style.RESET_ALL}")
        return instr(ins, p1, p2, write, res.sw2)
    elif res.sw1 == 0x61:  # GET RESPONSE
        print(f"{Fore.YELLOW}*** Retrying with GET RESPONSE{Style.RESET_ALL}")
        return instr(cla=0xA0, ins=0xC0, p1=0, p2=0, recv=res.sw2)
    print()
    return res


@dataclass
class Response:
    data: bytes
    sw1: int
    sw2: int

    @property
    def sw(self):
        return self.sw1 << 8 | self.sw2


class Card:
    def hello():
        return instr(0x01)

    def login(pin):
        assert len(pin) == 4
        return instr(0x02, write=list(map(int, pin)))

    def change_pin(new_pin):
        assert len(new_pin) == 4
        return instr(0x03, write=list(map(int, new_pin)))

    def logout():
        return instr(0x04)

    def factory_reset():
        return instr(0x05)

    def encrypt(data):
        assert len(data) <= 16
        if type(data) == str:
            data = data.encode("utf-8")
        return instr(0x06, write=data)

    def get_public_key():
        return instr(0x07)


def get_description(sw1, sw2):
    for s1, s2, desc in response_descriptions:
        if re.match(s1, "%02X" % sw1) and (m2 := re.match(s2, "%02X" % sw2)):
            if "." in s2:
                return re.sub(r"\\x+\\", str(int(m2.group(1), 16)), desc, flags=re.IGNORECASE)
            return desc
    return "Unknown response"


def query(*command):
    data, sw1, sw2 = connection.transmit(list(command))
    desc = get_description(sw1, sw2)
    print(" ".join("%02X" % c for c in command),
          "=>", "%02X %02X:" % (sw1, sw2), desc)
    if data:
        print(" ".join("%02X" % c for c in data),
              "==",
              bytes(data).decode("utf-8")  # display result as string
              # check if result only contains printable characters
              if all(32 <= c <= 127 for c in data)
              else "<binary>")  # if not then it's probably a blob
    return Response(data, sw1, sw2)


def init_card():
    global connection
    for reader in readers():
        try:
            connection = reader.createConnection()
            connection.connect()
            print(reader, toHexString(connection.getATR()))
            print()
            break
        except NoCardException:
            print(reader, 'no card inserted')
            exit()
    
    print(f"{Fore.GREEN}*** Creating applet command{Style.RESET_ALL}")
    instr(cla=0x80, ins=0xE8, p1=0x00, p2=0x00, write=APPLET_AID)
    print(f"{Fore.GREEN}*** Selecting applet{Style.RESET_ALL}")
    instr(cla=0x00, ins=0xA4, p1=0x04, p2=0x00, write=APPLET_AID)
