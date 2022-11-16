package notreprojet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;
import javacard.framework.Util;

/**
 * @noinspection ClassNamePrefixedWithPackageName, ImplicitCallToSuper,
 * MethodOverridesStaticMethodOfSuperclass, ResultOfObjectAllocationIgnored
 */
public class NotreProjet extends Applet {
    private static final byte[] hello =
        {0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x72, 0x6f, 0x62, 0x65, 0x72, 0x74};
    private static final byte PIN_LENGTH = 4;
    private static final byte MAX_PIN_TRY = 3;
    private final OwnerPIN pin;

    private static final byte CLA_PROJET = 0x42;

    private static final byte INS_LOGIN = 0x02;
    private static final byte INS_CHANGE_PIN = 0x03;
    private static final byte INS_LOGOUT = 0x04;
    private static final byte INS_FACTORY_RESET = 0x05;
    private static final byte INS_ENCRYPT = 0x06;
    private static final byte INS_GET_PUBLIC_KEY = 0x07;

    private static final short SW_PIN_FAILED = 0x63C0;

    protected NotreProjet() {
        register();
        pin = new OwnerPIN(MAX_PIN_TRY, PIN_LENGTH);
        pin.update(new byte[]{0x01, 0x02, 0x03, 0x04}, (short) 0, PIN_LENGTH);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new NotreProjet();
    }

    private void login(APDU apdu, byte[] buffer) {
        if (pin.getTriesRemaining() == 0) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        if (pin.check(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH)) {
            pin.resetAndUnblock();
        } else {
            ISOException.throwIt(SW_PIN_FAILED);
        }
    }

    private void logout(APDU apdu, byte[] buffer) {
        pin.reset();
    }

    /**
     * @noinspection UnusedDeclaration
     */
    public void process(APDU apdu) {
        if (selectingApplet()) {
            ISOException.throwIt(ISO7816.SW_NO_ERROR);
        }

        byte[] buffer = apdu.getBuffer();

        if ((buffer[ISO7816.OFFSET_CLA] == 0) && (buffer[ISO7816.OFFSET_INS] == (byte) 0xA4)) {
            return;
        }

        if (buffer[ISO7816.OFFSET_CLA] != CLA_PROJET) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        byte ins = buffer[ISO7816.OFFSET_INS];

        short bytesLeft = Util.makeShort((byte) 0x00, buffer[ISO7816.OFFSET_LC]);
        if (bytesLeft != apdu.setIncomingAndReceive())
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

        switch(ins) {

        }
    }
}