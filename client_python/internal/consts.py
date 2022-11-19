# noinspection PyUnresolvedReferences
from .colors import *

CLA_PROJET = 0x42
APPLET_AID = [0xA0, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x10, 0x01]

SW_COMMAND_SUCCESS = 0x9000
SW_SECURITY_STATUS_NOT_SATISFIED = 0x6982
SW_BLOCKED = 0x6983

INS_HELLO = 0x01
INS_LOGIN = 0x02
INS_CHANGE_PIN = 0x03
INS_LOGOUT = 0x04
INS_FACTORY_RESET = 0x05
INS_SIGN = 0x06
INS_GET_PUBLIC_KEY = 0x07
INS_GET_PRIVATE_KEY = 0x08

INS_LOAD = 0xE8
INS_SELECT = 0xA4


def get_instruction_name(ins):
    results = [key for key, val in globals().items() if val == ins and key.startswith("INS_")]
    if results:
        return results[0]
    return f"{ins:02X}"
