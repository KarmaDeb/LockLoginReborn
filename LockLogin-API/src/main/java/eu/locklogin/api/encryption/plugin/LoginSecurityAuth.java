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

import eu.locklogin.api.encryption.libraries.bcrypt.BCryptLib;

/**
 * LoginSecurity default authentication
 */
public final class LoginSecurityAuth {

    /**
     * Check the password with the specified token
     *
     * @param password the password
     * @param token    the token
     * @return if the password is correct
     */
    public static boolean check(String password, String token) {
        try {
            return BCryptLib.checkpw(password, token);
        } catch (IllegalArgumentException ignored) {}

        return false;
    }
}
