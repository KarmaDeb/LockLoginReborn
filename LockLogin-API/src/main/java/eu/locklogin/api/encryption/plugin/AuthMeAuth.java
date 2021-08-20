package eu.locklogin.api.encryption.plugin;

/*
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

import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * AuthMe default authentication
 */
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
