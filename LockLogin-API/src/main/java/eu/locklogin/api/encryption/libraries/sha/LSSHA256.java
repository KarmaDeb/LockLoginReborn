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

import java.nio.charset.StandardCharsets;

/**
 * SHA256 utilities
 */
public final class LSSHA256 {

    private final Object password;

    /**
     * Initialize the codification
     *
     * @param value  the value to codify
     */
    public LSSHA256(final Object value) {
        this.password = value;
    }

    /**
     * Check if the specified password
     * is correct
     *
     * @param token the provided token password
     * @return a boolean
     */
    public boolean check(final String token) {
        return token.equals(hash());
    }

    /**
     * hashEncrypted and encrypt the value
     *
     * @return a hashed value
     */
    @SuppressWarnings("all")
    public String hash() {
        return "$SHA256$" + Hashing.sha256().hashString(password.toString(), StandardCharsets.UTF_8).toString();
    }
}
