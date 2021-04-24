package ml.karmaconfigs.locklogin.api.encryption.sha;

import com.google.common.hash.Hashing;
import ml.karmaconfigs.locklogin.api.encryption.EncryptionMethod;

import java.nio.charset.StandardCharsets;

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
public final class SHA256Util extends EncryptionMethod {

    private final String password;
    private final String token;

    /**
     * Initialize the codification
     *
     * @param key the player password
     * @param userToken the user hashed password
     */
    public SHA256Util(final String key, final String userToken) {
        password = key;
        token = userToken;
    }

    /**
     * Hash the password
     *
     * @return the hashed password
     */
    @Override
    @SuppressWarnings("all")
    public final String hash() {
        return Hashing.sha256().hashString(password, StandardCharsets.UTF_8).toString();
    }

    /**
     * Check if the specified password
     * is correct
     *
     * @return a boolean
     */
    @Override
    @SuppressWarnings("all")
    public final boolean check() {
        String hashed_pw = Hashing.sha256().hashString(password, StandardCharsets.UTF_8).toString();

        return hashed_pw.equals(token);
    }
}