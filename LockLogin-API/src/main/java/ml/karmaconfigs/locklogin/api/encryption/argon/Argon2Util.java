package ml.karmaconfigs.locklogin.api.encryption.argon;

import ml.karmaconfigs.locklogin.api.encryption.EncryptionMethod;
import ml.karmaconfigs.locklogin.api.encryption.argon.util.Argon2;
import ml.karmaconfigs.locklogin.api.encryption.argon.util.model.Argon2Type;
import ml.karmaconfigs.locklogin.api.encryption.CryptType;

import java.nio.charset.StandardCharsets;

/**
 * LockLogin Argon2 utilities
 */
public final class Argon2Util extends EncryptionMethod {

    private final String password;
    private final String token;

    private final CryptType type;

    /**
     * Argon 2 utils class
     *
     * @param key the password/token to use
     * @param userToken the user hashed password
     * @param argonType the argon2 type to use
     */
    public Argon2Util(final String key, final String userToken, final CryptType argonType) {
        password = key;
        token = userToken;
        type = argonType;
    }

    /**
     * Hash to argon password
     *
     * @return the hashed password if it's not argon
     */
    @Override
    public final String hash() {
        switch (type) {
            case ARGON2I:
                Argon2 argon2i = Argon2.create().type(Argon2Type.Argon2i);
                return argon2i.memory(1024).parallelism(22).iterations(2).password(password.getBytes(StandardCharsets.UTF_8)).hash().getEncoded();
            case ARGON2ID:
                Argon2 argon2id = Argon2.create().type(Argon2Type.Argon2id);
                return argon2id.memory(1024).parallelism(22).iterations(2).password(password.getBytes(StandardCharsets.UTF_8)).hash().getEncoded();
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
            switch (type) {
                case ARGON2I:
                case ARGON2ID:
                    return Argon2.checkHash(token, password.getBytes(StandardCharsets.UTF_8));
                default:
                    return false;
            }
        }

        return false;
    }
}
