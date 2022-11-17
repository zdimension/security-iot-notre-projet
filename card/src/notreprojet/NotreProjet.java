package notreprojet;

import javacard.security.KeyPair;
import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;
import javacard.framework.Util;
import javacard.security.PublicKey;
import javacard.security.RSAPublicKey;
import javacardx.crypto.Cipher;

/**
 * @noinspection ClassNamePrefixedWithPackageName, ImplicitCallToSuper,
 * MethodOverridesStaticMethodOfSuperclass, ResultOfObjectAllocationIgnored
 */
public class NotreProjet extends Applet {
    private static final byte[] hello = {0x56, 0x69, 0x72, 0x74, 0x75, 0x61, 0x6c, 0x62, 0x6f, 0x78,
        0x20, 0x6d, 0x27, 0x61, 0x20, 0x74, 0x75, 0x65, 0x72};
    private static final byte PIN_LENGTH = 4;
    private static final byte MAX_PIN_TRY = 3;
    private final OwnerPIN pin;

    private static final byte CLA_PROJET = 0x42;

    private static final byte INS_HELLO = 0x01;
    private static final byte INS_LOGIN = 0x02;
    private static final byte INS_CHANGE_PIN = 0x03;
    private static final byte INS_LOGOUT = 0x04;
    private static final byte INS_FACTORY_RESET = 0x05;
    private static final byte INS_ENCRYPT = 0x06;
    private static final byte INS_GET_PUBLIC_KEY = 0x07;

    private static final short SW_PIN_FAILED_MORE = (short) 0x9704;
    private static final short SW_PIN_FAILED_2 = (short) 0x9A04;
    private static final short SW_PIN_FAILED_1 = (short) 0x9904;
    private static final short SW_BLOCKED = (short) 0x6983;

    private KeyPair keyPair;

    protected NotreProjet() {
        register();
        pin = new OwnerPIN(MAX_PIN_TRY, PIN_LENGTH);
        factoryReset();
    }

    /**
     * Generates the 512-bit RSA key pair and stores it in the card.
     */
    private void generateKeyPair() {
        try {
            keyPair = new KeyPair(KeyPair.ALG_RSA_CRT, (short) 512);
            keyPair.genKeyPair();
        } catch (Exception e) {
            ISOException.throwIt(ISO7816.SW_UNKNOWN);
        }
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new NotreProjet();
    }

    private void checkPin(byte[] bArray) {
        if (!pin.check(bArray, ISO7816.OFFSET_CDATA, PIN_LENGTH)) {
            short code;
            switch (pin.getTriesRemaining()) {
                case 2:
                    code = SW_PIN_FAILED_2;
                    break;
                case 1:
                    code = SW_PIN_FAILED_1;
                    break;
                case 0:
                    code = SW_BLOCKED;
                    break;
                default:
                    code = SW_PIN_FAILED_MORE;
                    break;
            }
            ISOException.throwIt(code);
        }
    }

    private void checkLoggedIn() {
        if (!pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
    }

    private void login(APDU apdu) {
        if (pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();
        if (pin.getTriesRemaining() == 0) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        checkPin(buffer);
    }

    private void logout(APDU apdu) {
        checkLoggedIn();

        pin.reset();
    }

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

        short bytesLeft = Util.makeShort((byte) 0x00, buffer[ISO7816.OFFSET_LC]);
        if (bytesLeft != apdu.setIncomingAndReceive())
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

        byte ins = buffer[ISO7816.OFFSET_INS];

        switch (ins) {
            case INS_HELLO:
                hello(apdu);
                break;
            case INS_LOGIN:
                login(apdu);
                break;
            case INS_LOGOUT:
                logout(apdu);
                break;
            case INS_CHANGE_PIN:
                changePin(apdu);
                break;
            case INS_FACTORY_RESET:
                factoryReset();
                break;
            case INS_ENCRYPT:
                encrypt(apdu);
                break;
            case INS_GET_PUBLIC_KEY:
                getPublicKey(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                break;
        }
    }

    private void hello(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(hello, (short) 0, buffer, ISO7816.OFFSET_CDATA, (short) hello.length);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short) hello.length);
    }

    private void getPublicKey(APDU apdu) {
        checkLoggedIn();

        byte[] buffer = apdu.getBuffer();
        short offset = ISO7816.OFFSET_CDATA;
        RSAPublicKey key = (RSAPublicKey) keyPair.getPublic();
        short expLen = key.getExponent(buffer, (short) (offset + 2));
        Util.setShort(buffer, offset, expLen);
        short modLen = key.getModulus(buffer, (short) (offset + 4 + expLen));
        Util.setShort(buffer, (short) (offset + 2 + expLen), modLen);
        apdu.setOutgoingAndSend(offset, (short) (4 + expLen + modLen));
    }

    private void encrypt(APDU apdu) {
        checkLoggedIn();

        byte[] buffer = apdu.getBuffer();
        Cipher cipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
        cipher.init(keyPair.getPrivate(), Cipher.MODE_ENCRYPT);
        byte dataLength = buffer[ISO7816.OFFSET_LC];
        short outLength = cipher.doFinal(buffer, ISO7816.OFFSET_CDATA, (short) dataLength, buffer, ISO7816.OFFSET_CDATA);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, outLength);
    }

    private void factoryReset() {
        setDefaultPin();
        generateKeyPair();
    }

    private void setDefaultPin() {
        pin.update(new byte[]{0x01, 0x02, 0x03, 0x04}, (short) 0, PIN_LENGTH);
    }

    private void changePin(APDU apdu) {
        checkLoggedIn();

        byte[] buffer = apdu.getBuffer();
        pin.update(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH);
    }
}