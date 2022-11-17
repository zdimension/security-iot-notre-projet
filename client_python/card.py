from dataclasses import dataclass
from smartcard.Exceptions import NoCardException
from smartcard.System import readers
from smartcard.util import toHexString
import re

with open("response_descriptions.txt") as fp:
    response_descriptions = [line.strip().split("\t") for line in fp]

CLA_PROJET = 0x42


def instr(ins, p1=0, p2=0, write=[], recv=0, cla=CLA_PROJET):
    res = query(cla, ins, p1, p2, *
                ([len(write), *write] if write else []), recv)
    if res.sw1 == 0x6C:
        print("*** Retrying with correct length")
        return instr(ins, p1, p2, write, res.sw2)
    elif res.sw1 == 0x61:  # GET RESPONSE
        print("*** Retrying with GET RESPONSE")
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


def query(*command):
    data, sw1, sw2 = connection.transmit(list(command))
    desc = [desc for s1, s2, kind, desc in response_descriptions if re.match(
        s1, "%02X" % sw1) and re.match(s2, "%02X" % sw2)]
    if desc == []:
        desc = "Unknown response"
    else:
        desc = desc[0]
    print(" ".join("%02X" % c for c in command),
          "=>", "%02X %02X" % (sw1, sw2), desc)
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
    # create applet cmd
    # our AID is 0xa0:0x40:0x41:0x42:0x43:0x44:0x45:0x46:0x10:0x01
    instr(cla=0x80, ins=0xE8, p1=0x00, p2=0x00, write=[
          0xA0, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x10, 0x01])
    # select applet cmd
    instr(cla=0x00, ins=0xA4, p1=0x04, p2=0x00, write=[
          0xA0, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x10, 0x01])
