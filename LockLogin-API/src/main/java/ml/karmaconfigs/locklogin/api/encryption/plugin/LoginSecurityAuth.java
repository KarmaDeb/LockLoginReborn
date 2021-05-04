package ml.karmaconfigs.locklogin.api.encryption.plugin;

import ml.karmaconfigs.locklogin.api.encryption.libraries.bcrypt.BCryptLib;

public class LoginSecurityAuth {

    /**
     * Check the password with the specified token
     *
     * @param password the password
     * @param token    the token
     * @return if the password is correct
     */
    public static boolean check(String password, String token) {
        return BCryptLib.checkpw(password, token);
    }
}
