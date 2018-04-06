package controllers.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by hefeng on 15/8/10.
 * Util to generate Agora media dynamic key.
 */
public class DynamicKeyUtil {

	public static byte[] encodeHMAC(String key, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException {
        return encodeHMAC(key.getBytes(), message);
    }

    public static byte[] encodeHMAC(byte[] key, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec keySpec = new SecretKeySpec(key, "RAW");

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(keySpec);
        return mac.doFinal(message);
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
