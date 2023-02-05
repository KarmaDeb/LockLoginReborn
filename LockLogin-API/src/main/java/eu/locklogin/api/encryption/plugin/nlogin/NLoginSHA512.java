package eu.locklogin.api.encryption.plugin.nlogin;

import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * nLogin like SHA512
 */
public final class NLoginSHA512 {

    private final String pwd;

    /**
     * Initialize the nLogin sha
     *
     * @param password the password to use
     */
    public NLoginSHA512(final String password) {
        pwd = password;
    }

    /**
     * Validate the nLogin sha512 hash
     *
     * @param token the token
     * @return if the password is valid
     */
    public boolean validate(final String token) {
        if (token.contains("$")) {
            String[] data = token.split("\\$");
            String salt = data[data.length - 1];

            String first_hash = hash(pwd);
            String second_hash = hash(first_hash + salt);

            return ("$SHA512$" + second_hash + "$" + salt).equals(token);
        } else {
            return false;
        }
    }

    /**
     * Hash the password using native sha512
     *
     * @param k the password
     * @return the hashed password
     */
    private String hash(final String k) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(k.getBytes());
            byte[] output = digest.digest();
            return String.format("%0" + (output.length << 1) + "x", new BigInteger(1, output));
        } catch (Throwable ex) {
            return "";
        }
    }
}
