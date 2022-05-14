package eu.locklogin.api.encryption.libraries.wordpress;

import eu.locklogin.api.encryption.libraries.sha.SHA512;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * WordPress crypto
 */
public final class WordPressCrypt {

    private final String password;

    private static final String chars = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private final SecureRandom generator = new SecureRandom();

    /**
     * Initialize the WordPress cryptography
     *
     * @param pwd the password
     */
    public WordPressCrypt(final String pwd) {
        password = pwd;
    }

    /**
     * Hash the password
     *
     * @return the hashed password
     */
    public String hash() {
        return encrypt(generateSetting());
    }

    /**
     * Validate the password with the provided hash
     *
     * @param hash the hash
     * @return if the password is valid
     */
    public boolean validate(final String hash) {
        String hashed = encrypt(hash);
        return MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8), hashed.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hash the password
     *
     * @return the hashed password
     */
    private String encrypt(final String setting) {
        String result = "*0";

        if (((setting.length() < 2) ? setting : setting.substring(0, 2)).equalsIgnoreCase(result)) {
            result = "*1";
        }

        String id = (setting.length() < 3) ? setting : setting.substring(0, 3);
        if (!(id.equals("$P$") || id.equals("$H$"))) {
            return result;
        }

        int countLog2 = chars.indexOf(setting.charAt(3));
        if (countLog2 < 7 || countLog2 > 30) {
            return result;
        }

        int count = 1 << countLog2;

        String salt = setting.substring(4, 4 + 8);
        if (salt.length() != 8) {
            return result;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] pass = password.getBytes(StandardCharsets.UTF_8);
            byte[] hash = md.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            do {
                byte[] t = new byte[hash.length + pass.length];
                System.arraycopy(hash, 0, t, 0, hash.length);
                System.arraycopy(pass, 0, t, hash.length, pass.length);
                hash = md.digest(t);
            } while (--count > 0);

            result = setting.substring(0, 12);
            result += itoa64(hash, 16);
            return result;
        } catch (Throwable ex) {
            ex.printStackTrace();
            SHA512 tmp = new SHA512();
            return tmp.hash(password);
        }
    }

    /**
     * Return the itoa64 value of the data
     *
     * @param data the data
     * @param size the preferred size
     * @return the itoa64 value representation in string
     */
    private String itoa64(byte[] data, final int size) {
        int i, value;
        StringBuilder output = new StringBuilder();
        i = 0;

        if (data.length < size) {
            byte[] t = new byte[size];
            System.arraycopy(data, 0, t, 0, data.length);
            Arrays.fill(t, data.length, size - 1, (byte) 0);
            data = t;
        }

        do {
            value = data[i] + (data[i] < 0 ? 256 : 0);
            ++i;
            output.append(chars.charAt(value & 63));
            if (i < size) {
                value |= (data[i] + (data[i] < 0 ? 256 : 0)) << 8;
            }
            output.append(chars.charAt((value >> 6) & 63));
            if (i++ >= size) {
                break;
            }
            if (i < size) {
                value |= (data[i] + (data[i] < 0 ? 256 : 0)) << 16;
            }
            output.append(chars.charAt((value >> 12) & 63));
            if (i++ >= size) {
                break;
            }
            output.append(chars.charAt((value >> 18) & 63));
        } while (i < size);

        return output.toString();
    }

    /**
     * Generate a random salt value
     *
     * @return the random salt
     */
    private String generateSetting() {
        byte[] random = new byte[6];
        generator.nextBytes(random);
        String tmp = new String(random);
        byte[] input = tmp.getBytes(StandardCharsets.UTF_8);

        String output = "$P$";
        int interactions = 8;

        output += chars.charAt(Math.min(interactions + 5, 30));
        output += itoa64(input, 6);

        return output;
    }
}
