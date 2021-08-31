package eu.locklogin.api.encryption.libraries.sha;

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

import com.google.common.hash.Hashing;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA512 utilities
 *
 * This is used specially to add DynamicBungeeAuth compatibility
 */
@SuppressWarnings("UnstableApiUsage")
public final class SHA512X {

    private final String pwd;

    /**
     * Initialize the SHA512 - X hash
     *
     * @param password the password
     */
    public SHA512X(final String password) {
        pwd = password;
    }

    /**
     * Validate the hash
     *
     * @param token the already hashed value
     * @param salt the salt to use
     * @return if the password matches the token
     */
    public boolean validate(final String token, final String salt) {
        if (validateC(token, salt)) {
            return true;
        } else {
            return validateH(token, salt);
        }
    }

    /**
     * Get if the password matches with the token
     *
     * @param salt the password salt
     * @return if the password is valid
     */
    private boolean validateH(final String token, final String salt) {
        String password = Hashing.sha512().hashString(token, StandardCharsets.UTF_8).toString();
        return Hashing.sha512().hashString(password + salt, StandardCharsets.UTF_8).toString().equals(token);
    }

    /**
     * Get if the password matches with the token
     *
     * @param salt the password salt
     * @return if the password is valid
     */
    private boolean validateC(final String token, final String salt) {
        if (salt != null) {
            String str = sha512c(pwd);
            return sha512c(str + salt).equals(token);
        }
        return sha512c(pwd).equals(token);
    }

    /**
     * Hash using message digest
     *
     * @param value the value to hash
     * @return the hashed value
     */
    private String sha512c(final String value) {
        String str = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            messageDigest.reset();
            messageDigest.update(value.getBytes());
            byte[] arrayOfByte = messageDigest.digest();
            str = String.format("%0" + (arrayOfByte.length << 1) + "x", new BigInteger(1, arrayOfByte));
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return str;
    }
}
