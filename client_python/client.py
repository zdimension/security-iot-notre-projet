from smartcard.Exceptions import NoCardException
from smartcard.System import readers
from smartcard.util import toHexString
import sys

for reader in readers():
    try:
        connection = reader.createConnection()
        connection.connect()
        print(reader, toHexString(connection.getATR()))
        break
    except NoCardException:
        print(reader, 'no card inserted')
        exit


# create applet cmd
# our AID is 0xa0:0x40:0x41:0x42:0x43:0x44:0x45:0x46:0x10:0x01
data, sw1, sw2 = connection.transmit([0x80, 0xE8, 0x00, 0x00, 0x0A, 0xA0, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x10, 0x01])
print('create applet cmd: %s %02X %02X' % (toHexString(data), sw1, sw2))
# select applet cmd
data, sw1, sw2 = connection.transmit([0x00, 0xA4, 0x04, 0x00, 0x0A, 0xA0, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x10, 0x01])
print('select applet cmd: %s %02X %02X' % (toHexString(data), sw1, sw2))

# send INS 0x40 for result length 12
data, sw1, sw2 = connection.transmit([0x00, 0x40, 0x00, 0x00, 0x0C])
print('INS 0x40: %s %s %02X %02X' % (toHexString(data), "".join(map(chr, data)), sw1, sw2))