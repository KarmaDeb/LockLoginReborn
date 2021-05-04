package ml.karmaconfigs.locklogin.api.encryption.argon;

import ml.karmaconfigs.locklogin.api.encryption.HashType;
import ml.karmaconfigs.locklogin.api.encryption.libraries.argon.Argon2;
import ml.karmaconfigs.locklogin.api.encryption.libraries.argon.model.Argon2Type;

import java.nio.charset.StandardCharsets;

/**
 * LockLogin Argon2 utilities
 */
public final class Argon2Util {

    private final String password;

    /**
     * Argon 2 utils class
     *
     * @param key the password/token to use
     */
    public Argon2Util(final String key) {
        password = key;
    }

    /**
     * Hash to argon password
     *
     * @param type the argon password type
     * @return the hashed password if it's not argon
     */
    public final String hashPassword(final HashType type) {
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
     * @param token the player hashed password
     * @param type  the encryption type
     * @return if the password matches
     */
    public final boolean checkPassword(final String token, final HashType type) {
        switch (type) {
            case ARGON2I:
            case ARGON2ID:
                return Argon2.checkHash(token, password.getBytes(StandardCharsets.UTF_8));
            default:
                return false;
        }
    }
}
