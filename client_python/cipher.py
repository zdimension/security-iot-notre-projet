import sys

from colorama import init

from internal.card import init_card, Logger
from internal.consts import *

init()

# get filename from argv
if len(sys.argv) < 2:
    print(f"Usage: python3 ", GREEN('cipher.py'), YELLOW('<filename>'), LIGHTBLACK_EX('[-v]'))
    exit()

filename = sys.argv[1]

if "-v" in sys.argv:
    Logger.log_verbose = True

card = init_card()

# read file
with open(filename, "rb") as f:
    data = f.read()

if filename.endswith(".sign"):
    print("Verifying signature...")
    print(card.verify(filename[:-5]))
    print()
    input("Press enter to exit...")
else:
    while True:
        pin = input("PIN? (default is 1234) ")
        res = card.login(list(map(int, pin)))
        if res.status.is_success:
            break
        if res.status.sw in (SW_SECURITY_STATUS_NOT_SATISFIED, SW_BLOCKED):
            if input("Card blocked, reset? [y/N] ").lower() == "y":
                card.factory_reset()
            else:
                exit()

    print("Signing...")
    data = bytes(card.sign(data).data)
    filename += ".sign"

    # write file
    with open(filename, "wb") as f:
        f.write(data)
