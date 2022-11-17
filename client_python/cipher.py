from card import Card, init_card
import sys
import rsa

# get filename from argv
if len(sys.argv) != 2:
    print("Usage: python3 cipher.py <filename>")
    exit()

filename = sys.argv[1]

init_card()

SW_COMMAND_SUCCESS = 0x9000
SW_SECURITY_STATUS_NOT_SATISFIED = 0x6982
SW_BLOCKED = 0x6983

while True:
    pin = input("PIN? (default is 1234) ")
    res = Card.login(list(map(int, pin)))
    if res.sw == SW_COMMAND_SUCCESS:
        break
    if res.sw in (SW_SECURITY_STATUS_NOT_SATISFIED, SW_BLOCKED):
        if input("Card blocked, reset? [y/N] ").lower() == "y":
            Card.factory_reset()
        else:
            exit()

# read file
with open(filename, "rb") as f:
    data = f.read()

if filename.endswith(".enc"):
    print("Decrypting...")
    # format is [exp len (u16), exp (u8*exp len), mod len (u16), mod (u8*mod len)]
    pubkey = Card.get_public_key()

    # deserialize public key
    exp_len = pubkey.data[0] << 8 | pubkey.data[1]
    exp = int.from_bytes(pubkey.data[2:2+exp_len], "big")
    mod_len = pubkey.data[2+exp_len] << 8 | pubkey.data[2+exp_len+1]
    mod = int.from_bytes(pubkey.data[2+exp_len+2:2+exp_len+2+mod_len], "big")

    # decrypt
    data = rsa.core.decrypt_int(int.from_bytes(
        data, "big"), exp, mod).to_bytes(mod_len, "big")

    # check cleartext marker
    if data[:2] != b"\x00\x01":
        print("Invalid padding (wrong kind)")
        exit()

    # find padding separator
    sep_idx = data.find(b"\x00", 2)
    if sep_idx < 10:
        print("Invalid padding (too short)")
        exit()

    # remove padding
    data = data[sep_idx+1:]
    filename = filename[:-4]
else:
    print("Encrypting...")
    data = bytes(Card.encrypt(data).data)
    filename += ".enc"

# write file
with open(filename, "wb") as f:
    f.write(data)
input()
