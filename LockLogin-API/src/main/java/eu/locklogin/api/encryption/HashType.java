package eu.locklogin.api.encryption;

import java.util.Random;

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
public enum HashType {
    /**
     * LockLogin compatible hash type
     */
    LS_SHA256,
    /**
     * LockLogin compatible hash type
     */
    SHA256,
    /**
     * LockLogin compatible hash type
     */
    SHA512,
    /**
     * LockLogin compatible hash type
     */
    BCrypt,
    /**
     * LockLogin compatible hash type
     */
    BCryptPHP,
    /**
     * LockLogin compatible hash type
     */
    ARGON2I,
    /**
     * LockLogin compatible hash type
     */
    ARGON2ID,
    /**
     * LockLogin compatible hash type
     */
    AUTHME_SHA,
    /**
     * LockLogin compatible hash type
     */
    WORDPRESS,
    /**
     * LockLogin unknown hash type
     */
    UNKNOWN,
    /**
     * No encryption type
     */
    NONE;

    /**
     * Pick a random hash type
     *
     * @return the hash type
     */
    public static HashType pickRandom() {
        HashType[] valid = new HashType[]{
                SHA256,
                SHA512,
                BCrypt,
                ARGON2I,
                ARGON2ID,
                WORDPRESS /*Should I really implement this?*/
        };

        return valid[new Random().nextInt(valid.length)];
    }
}
