package eu.locklogin.api.encryption.libraries;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Digest authentication tester
 */
public class DigestTester {

    private final static String[] SUPPORTED_DIGESTS = new String[]{
            "SHA-512",
            "SHA-256",
            "MD5"
    };

    /**
     * Check the token
     *
     * @param token the token
     * @param password the password
     * @return if the hash matches
     */
    public static boolean check(final String token, final String password) {
        String baseToken = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));

        for (String method : SUPPORTED_DIGESTS) {
            try {
                MessageDigest digest = MessageDigest.getInstance(method);
                byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

                String baseHash = Base64.getEncoder().encodeToString(hash);
                if (isBase64(token)) {
                    if (baseHash.equals(token)) return true; //Check directly with the token
                }

                if (baseHash.equals(baseToken)) return true;
            } catch (NoSuchAlgorithmException ignored) {}
        }

        return false;
    }

    private static boolean isBase64(final String data) {
        String regex =
                "([A-Za-z0-9+/]{4})*" +
                        "([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)";

        Pattern patron = Pattern.compile(regex);
        return patron.matcher(data).matches();
    }

    private static boolean isHex(final String str) {
        if (str.length() % 2 != 0) {
            return false;
        }

        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c) && !(c >= 'a' && c <= 'f') && !(c >= 'A' && c <= 'F')) {
                return false;
            }
        }

        return true;
    }
}
