package ml.karmaconfigs.locklogin.api.encryption.plugins.authme;

import ml.karmaconfigs.locklogin.api.encryption.EncryptionMethod;

import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */
public class AuthMeAuth extends EncryptionMethod {

    private final String key;
    private final String token;

    /**
     * Initialize the authme auth class
     *
     * @param password the player password
     * @param userPassword the player stored password
     */
    public AuthMeAuth(final String password, final String userPassword) {
        key = password;
        token = userPassword;
    }

    /**
     * Check the password with the specified token
     *
     * @return if the password is correct
     */
    @Override
    public final boolean check() {
        try {
            final String[] components = token.split("\\$");
            final String salt = components[2];
            final String compareHash = "$SHA$" + salt + "$" + hashSha256(hashSha256(key) + salt);
            return token.equals(compareHash);
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * hashEncrypted the password as Sha256
     *
     * @return a encrypted password
     */
    @Override
    public final String hash() {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            digest.update(key.getBytes());
            byte[] output = digest.digest();
            return String.format("%0" + (output.length << 1) + "x", new BigInteger(1, output));
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * hashEncrypted the password as Sha256
     *
     * @return a encrypted password
     */
    private String hashSha256(final String password) {
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
