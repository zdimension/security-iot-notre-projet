package notreprojet;

import java.math.BigInteger;
import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;
import javacard.framework.Util;
import javacard.security.KeyPair;
import javacard.security.RSAPrivateCrtKey;
import javacard.security.RSAPublicKey;
import javacardx.crypto.Cipher;

/**
 * @noinspection unused
 */
public class NotreProjet extends Applet {
    /**
     * Test string message
     */
    public static final byte[] TEST_STRING = {0x56, 0x69, 0x72, 0x74, 0x75, 0x61, 0x6c, 0x62, 0x6f,
        0x78,
        0x20, 0x6d, 0x27, 0x61, 0x20, 0x74, 0x75, 0x65, 0x72};
    /**
     * Number of digits in the PIN
     */
    public static final byte PIN_LENGTH = 4;
    /**
     * Maximum number of tries before the PIN is blocked
     */
    public static final byte MAX_PIN_TRY = 3;
    /**
     * Class number
     */
    public static final byte CLA_PROJET = 0x42;
    /**
     * @see NotreProjet#hello(APDU)
     */
    public static final byte INS_HELLO = 0x01;
    /**
     * @see NotreProjet#login(APDU)
     */
    public static final byte INS_LOGIN = 0x02;
    /**
     * @see NotreProjet#changePin(APDU)
     */
    public static final byte INS_CHANGE_PIN = 0x03;
    /**
     * @see NotreProjet#logout()
     */
    public static final byte INS_LOGOUT = 0x04;
    /**
     * @see NotreProjet#factoryReset()
     */
    public static final byte INS_FACTORY_RESET = 0x05;
    /**
     * @see NotreProjet#sign(APDU)
     */
    public static final byte INS_SIGN = 0x06;
    /**
     * @see NotreProjet#getPublicKey(APDU)
     */
    public static final byte INS_GET_PUBLIC_KEY = 0x07;
    /**
     * @see NotreProjet#getPrivateKey(APDU)
     */
    public static final byte INS_GET_PRIVATE_KEY = 0x08;
    /**
     * PIN failed, more than 3 tries remaining
     */
    public static final short SW_PIN_FAILED_MORE = (short) 0x9704;
    /**
     * PIN failed, 2 tries remaining
     */
    public static final short SW_PIN_FAILED_2 = (short) 0x9A04;
    /**
     * PIN failed, 1 try remaining
     */
    public static final short SW_PIN_FAILED_1 = (short) 0x9904;
    /**
     * PIN failed, card blocked
     */
    public static final short SW_BLOCKED = (short) 0x6983;
    /**
     * Default PIN (1234)
     */
    public static final byte[] DEFAULT_PIN = {0x01, 0x02, 0x03, 0x04};
    private static final short KEY_BITS = 512;
    /**
     * PIN
     */
    private final OwnerPIN pin;
    /*
     * RSA key pair
     */
    private KeyPair keyPair;

    protected NotreProjet() {
        register();
        pin = new OwnerPIN(MAX_PIN_TRY, PIN_LENGTH);
        factoryReset();
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new NotreProjet();
    }

    /**
     * Generates the 512-bit RSA key pair and stores it in the card.
     */
    private void generateKeyPair() {
        keyPair = new KeyPair(KeyPair.ALG_RSA_CRT, KEY_BITS);
        keyPair.genKeyPair();
    }

    /**
     * <p>Checks that the provided PIN is correct.</p>
     * <p>If the length is incorrect, {@link ISO7816#SW_WRONG_LENGTH} is thrown.</p>
     * <p>If the PIN is incorrect, one of the following is thrown depending on the number of tries
     * remaining:</p>
     * <ul>
     *     <li>{@link #SW_PIN_FAILED_MORE}</li>
     *     <li>{@link #SW_PIN_FAILED_2}</li>
     *     <li>{@link #SW_PIN_FAILED_1}</li>
     *     <li>{@link #SW_BLOCKED}</li>
     * </ul>
     *
     * @param bArray the APDU buffer
     */
    private void checkPin(byte[] bArray) {
        if (bArray[ISO7816.OFFSET_LC] != PIN_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

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

    /**
     * <p>Checks if the user is logged in.</p>
     * <p>If not, throws {@link ISO7816#SW_SECURITY_STATUS_NOT_SATISFIED}.</p>
     */
    private void checkLoggedIn() {
        if (!pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
    }


    /**
     * <h2>Main APDU entry point.</h2>
     * <p>Checks if the applet is being selected; if so, throws {@link ISO7816#SW_NO_ERROR}.</p>
     * <p>Checks if the APDU query is SELECT file; if so, returns.</p>
     * <p>Checks if the APDU query is for a different class; if so, throws
     * {@link ISO7816#SW_CLA_NOT_SUPPORTED}.</p>
     * <p>Ensures the buffer is fully received; otherwise, throws
     * {@link ISO7816#SW_WRONG_LENGTH}.</p>
     * <p>If everything went well, program flow is delegated to the appropriate method.</p>
     *
     * @param apdu the APDU object
     */
    public void process(APDU apdu) {
        if (selectingApplet()) {
            // ignore if the applet is being selected
            ISOException.throwIt(ISO7816.SW_NO_ERROR);
        }

        byte[] buffer = apdu.getBuffer();

        if ((buffer[ISO7816.OFFSET_CLA] == 0) && (buffer[ISO7816.OFFSET_INS] == (byte) 0xA4)) {
            // return if this is a SELECT FILE command
            return;
        }

        if (buffer[ISO7816.OFFSET_CLA] != CLA_PROJET) {
            // throw exception if the CLA byte is not ours
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        // ensure the entirety of the buffer is available for the processing
        short bytesLeft = Util.makeShort((byte) 0x00, buffer[ISO7816.OFFSET_LC]);
        if (bytesLeft != apdu.setIncomingAndReceive()) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        byte ins = buffer[ISO7816.OFFSET_INS];

        // dispatch to the appropriate handler
        switch (ins) {
            case INS_HELLO:
                hello(apdu);
                break;
            case INS_LOGIN:
                login(apdu);
                break;
            case INS_LOGOUT:
                logout();
                break;
            case INS_CHANGE_PIN:
                changePin(apdu);
                break;
            case INS_FACTORY_RESET:
                factoryReset();
                break;
            case INS_SIGN:
                sign(apdu);
                break;
            case INS_GET_PUBLIC_KEY:
                getPublicKey(apdu);
                break;
            case INS_GET_PRIVATE_KEY:
                getPrivateKey(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                break;
        }
    }

    /**
     * Resets the pin to {@link NotreProjet#DEFAULT_PIN}.
     */
    private void setDefaultPin() {
        pin.update(DEFAULT_PIN, (short) 0, PIN_LENGTH);
    }

    /**
     * <h2>"Hello" command.</h2>
     * <p>Sends back a test string ({@link NotreProjet#TEST_STRING}) to ensure communication is
     * working.</p>
     *
     * @param apdu the APDU object
     */
    private void hello(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        Util.arrayCopy(TEST_STRING, (short) 0, buffer, ISO7816.OFFSET_CDATA,
            (short) TEST_STRING.length);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (short) TEST_STRING.length);
    }

    /**
     * <h2>"Login" command.</h2>
     * <p>If the user is already logged in, throws {@link ISO7816#SW_CONDITIONS_NOT_SATISFIED}.</p>
     * <p>If the card is blocked, throws {@link ISO7816#SW_SECURITY_STATUS_NOT_SATISFIED}.</p>
     * <p>Checks whether the PIN is correct, and if so, logs the user in.</p>
     * <p>If the PIN is incorrect, an exception is thrown as described in
     * {@link #checkPin(byte[])}.</p>
     *
     * @param apdu the APDU object
     */
    private void login(APDU apdu) {
        if (pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }

        if (pin.getTriesRemaining() == 0) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        byte[] buffer = apdu.getBuffer();

        checkPin(buffer);
    }

    /**
     * <h2>"Change PIN" command.</h2>
     * <p>Checks that the user is logged in ({@link NotreProjet#checkLoggedIn()}), and if so,
     * changes the PIN to the one sent by the client.</p>
     * <p>Data must be exactly {@link NotreProjet#PIN_LENGTH} bytes long; if not,
     * {@link ISO7816#SW_WRONG_LENGTH} is thrown.</p>
     *
     * @param apdu the APDU object
     */
    private void changePin(APDU apdu) {
        checkLoggedIn();

        byte[] buffer = apdu.getBuffer();

        if (buffer[ISO7816.OFFSET_LC] != PIN_LENGTH) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        pin.update(buffer, ISO7816.OFFSET_CDATA, PIN_LENGTH);
    }

    /**
     * <h2>"Logout" command.</h2>
     * <p>Checks that the user is logged in ({@link NotreProjet#checkLoggedIn()}) , and if so, logs
     * the user out.</p>
     */
    private void logout() {
        checkLoggedIn();

        pin.reset();
    }

    /**
     * <h2>"Factory reset" command.</h2>
     * <p>Resets the applet to its initial state.</p>
     * <p>The PIN is reset to {@link NotreProjet#DEFAULT_PIN}, and the key pair is regenerated.</p>
     */
    private void factoryReset() {
        setDefaultPin();
        generateKeyPair();
    }

    private static final byte[] ASN1_SHA256 = {
        (byte) 0x30, (byte) 0x31, (byte) 0x30, (byte) 0x0d, (byte) 0x06, (byte) 0x09, (byte) 0x60,
        (byte) 0x86, (byte) 0x48, (byte) 0x01, (byte) 0x65, (byte) 0x03, (byte) 0x04, (byte) 0x02,
        (byte) 0x01, (byte) 0x05, (byte) 0x00, (byte) 0x04, (byte) 0x20
    };

    /**
     * <h2>"Sign" command.</h2>
     * <p>Checks that the user is logged in ({@link NotreProjet#checkLoggedIn()}), and if so,
     * signs the data sent by the client using the private key
     * ({@link NotreProjet#keyPair}).</p>
     *
     * @param apdu the APDU object
     */
    private void sign(APDU apdu) {
        checkLoggedIn();

        byte[] buffer = apdu.getBuffer();
        Cipher cipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
        cipher.init(keyPair.getPrivate(), Cipher.MODE_ENCRYPT);
        short inputLength = buffer[ISO7816.OFFSET_LC];
        byte[] markedData = new byte[(short)(inputLength + ASN1_SHA256.length)];
        Util.arrayCopy(ASN1_SHA256, (short) 0, markedData, (short) 0, (short) ASN1_SHA256.length);
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, markedData, (short) ASN1_SHA256.length, inputLength);
        byte dataLength = buffer[ISO7816.OFFSET_LC];
        short outLength = cipher.doFinal(markedData, (short) 0, (short) markedData.length, buffer, ISO7816.OFFSET_CDATA);
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, outLength);
    }

    /**
     * <h2>"Get public key" command.</h2>
     * <p>Sends back the public key ({@link NotreProjet#keyPair}) to the client.</p>
     * <p>Output format:</p>
     * <table>
     *     <tr><th>Offset</th><th>Length</th><th>Value</th></tr>
     *     <tr><td>0</td><td>2</td><td>Exponent length (<code>exp_len</code>)</td></tr>
     *     <tr><td>2</td><td><code>exp_len</code></td><td>Exponent</td></tr>
     *     <tr><td>2 + <code>exp_len</code></td><td>2</td><td>Modulus length (<code>mod_len</code>)</td></tr>
     *     <tr><td>4 + <code>exp_len</code></td><td><code>mod_len</code></td><td>Modulus</td></tr>
     * </table>
     *
     * @param apdu the APDU object
     */
    private void getPublicKey(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short offset = ISO7816.OFFSET_CDATA;
        RSAPublicKey key = (RSAPublicKey) keyPair.getPublic();
        short expLen = key.getExponent(buffer, (short) (offset + 2));
        Util.setShort(buffer, offset, expLen);
        short modLen = key.getModulus(buffer, (short) (offset + 4 + expLen));
        Util.setShort(buffer, (short) (offset + 2 + expLen), modLen);
        apdu.setOutgoingAndSend(offset, (short) (4 + expLen + modLen));
    }

    /**
     * <h2>"Get private key" command.</h2>
     * <p>Checks that the user is logged in ({@link NotreProjet#checkLoggedIn()}), and if so,
     * sends back the private key ({@link NotreProjet#keyPair}) to the client.</p>
     * <p>Output format:</p>
     * <table>
     *     <tr><th>Offset</th><th>Length</th><th>Value</th></tr>
     *     <tr><td>0</td><td>2</td><td>P length (<code>p_len</code>)</td></tr>
     *     <tr><td>2</td><td><code>p_len</code></td><td>P</td></tr>
     *     <tr><td>2 + <code>p_len</code></td><td>2</td><td>Q length (<code>q_len</code>)</td></tr>
     *     <tr><td>4 + <code>p_len</code></td><td><code>q_len</code></td><td>Q</td></tr>
     * </table>
     *
     * @param apdu the APDU object
     */
    private void getPrivateKey(APDU apdu) {
        checkLoggedIn();

        byte[] buffer = apdu.getBuffer();
        short offset = ISO7816.OFFSET_CDATA;
        RSAPrivateCrtKey key = (RSAPrivateCrtKey) keyPair.getPrivate();
        short pLen = key.getP(buffer, (short) (offset + 2));
        Util.setShort(buffer, offset, pLen);
        short qLen = key.getQ(buffer, (short) (offset + 4 + pLen));
        Util.setShort(buffer, (short) (offset + 2 + pLen), qLen);
        apdu.setOutgoingAndSend(offset, (short) (4 + pLen + qLen));
    }
}