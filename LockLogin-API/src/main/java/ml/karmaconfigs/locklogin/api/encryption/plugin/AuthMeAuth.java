package ml.karmaconfigs.locklogin.api.encryption.plugin;

import java.math.BigInteger;
import java.security.MessageDigest;

public class AuthMeAuth {

    /**
     * Check the password with the specified token
     *
     * @param password the password
     * @param token    the token
     * @return if the password is correct
     */
    public static boolean check(String password, String token) {
        try {
            final String[] components = token.split("\\$");
            final String salt = components[2];
            final String compareHash = "$SHA$" + salt + "$" + hashSha256(hashSha256(password) + salt);
            return token.equals(compareHash);
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * hashEncrypted the password as Sha256
     *
     * @param password the password
     * @return a encrypted password
     */
    private static String hashSha256(String password) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            digest.update(password.getBytes());
            byte[] output = digest.digest();
            return String.format("%0" + (output.length << 1) + "x", new BigInteger(1, output));
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
