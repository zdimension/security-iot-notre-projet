package notreprojet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

/**
 * @noinspection ClassNamePrefixedWithPackageName, ImplicitCallToSuper, MethodOverridesStaticMethodOfSuperclass, ResultOfObjectAllocationIgnored
 */
public class NotreProjet extends Applet {
    private final static byte[] hello=
        {0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x72, 0x6f, 0x62, 0x65, 0x72, 0x74} ;
    protected NotreProjet() {
        register();
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new NotreProjet();
    }

    /**
     * @noinspection UnusedDeclaration
     */
    public void process(APDU apdu) {
        // Good practice: Return 9000 on SELECT
        if (selectingApplet()) {
            return;
        }

        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
            case (byte) 0x40:
                Util.arrayCopy(hello, (byte)0, buf, ISO7816.OFFSET_CDATA, (byte)12);
                apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (byte)12);

                break;
            default:
                // good practice: If you don't know the INStruction, say so:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}