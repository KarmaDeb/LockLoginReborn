package ml.karmaconfigs.locklogin.api.encryption.bcrypt;

import ml.karmaconfigs.locklogin.api.encryption.EncryptionMethod;
import ml.karmaconfigs.locklogin.api.encryption.CryptType;

public final class BCryptUtil extends EncryptionMethod {

    private final String password;
    private final String token;

    private final CryptType type;

    /**
     * Initialize the BCrypt util
     *
     * @param key the user password
     * @param userToken the user hashed password
     * @param bcryptType the bcrypt type
     */
    public BCryptUtil(final String key, final String userToken, final CryptType bcryptType) {
        password = key;
        token = userToken;
        type = bcryptType;
    }

    /**
     * Hash the password
     *
     * @return the hashed password
     */
    @Override
    public final String hash() {
        switch (type) {
            case BCrypt:
                return BCryptHasher.hashpw(password, BCryptHasher.gensalt());
            case BCryptPHP:
                return BCryptHasher.hashpw(password, BCryptHasher.gensalt()).replaceFirst("2a", "2y");
            default:
                return password;
        }
    }

    /**
     * Check if the argon password matches with the specified token
     *
     * @return if the password matches
     */
    @Override
    public final boolean check() {
        if (token != null) {
            return BCryptHasher.checkpw(password, token.replaceFirst("2y", "2a"));
        }

        return false;
    }
}
