package ml.karmaconfigs.locklogin.api.encryption;

import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.locklogin.api.encryption.argon.Argon2Util;
import ml.karmaconfigs.locklogin.api.encryption.bcrypt.BCryptUtil;
import ml.karmaconfigs.locklogin.api.encryption.plugins.authme.AuthMeAuth;
import ml.karmaconfigs.locklogin.api.encryption.sha.SHA256Util;
import ml.karmaconfigs.locklogin.api.encryption.sha.SHA512Util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CryptoUtil {

    private final String password;
    private final String token;

    /**
     * Initialize the crypto util
     *
     * @param clientPassword the client password
     * @param clientToken the client token
     */
    public CryptoUtil(final String clientPassword, final String clientToken) {
        if (clientPassword != null)
            password = clientPassword;
        else
            password = "";

        if (clientToken != null)
            token = clientToken;
        else
            token = "";
    }

    /**
     * Returns the password in base 64 only
     *
     * @return the password in base 64
     */
    public final String toBase64(final boolean doToken) {
        if (!isBase64(doToken))
            return Base64.getEncoder().encodeToString((doToken ? token.getBytes(StandardCharsets.UTF_8) : password.getBytes(StandardCharsets.UTF_8)));

        return (doToken ? token : password);
    }

    /**
     * Returns the password decoded from base 64
     * if it was
     *
     * @return the decoded password
     */
    public final String fromBase64(final boolean doToken) {
        if (isBase64(doToken))
            return new String(Base64.getDecoder().decode((doToken ? token.getBytes(StandardCharsets.UTF_8) : password.getBytes(StandardCharsets.UTF_8))));

        return (doToken ? token : password);
    }

    /**
     * Get the encryption type of
     * the password
     *
     * @return the password encryption type
     */
    public final CryptType getType() {
        try {
            String[] token_data;
            if (isBase64(true))
                token_data = new String(Base64.getDecoder().decode(token)).split("\\$");
            else
                token_data = token.split("\\$");

            String type_backend = token_data[2];
            String type = token_data[1];

            CryptType crypto;

            switch (type.toLowerCase()) {
                case "sha512":
                case "512":
                    crypto = CryptType.SHA512;
                    break;
                case "sha256":
                case "256":
                    crypto = CryptType.SHA256;
                    break;
                case "2y":
                    crypto = CryptType.BCryptPHP;
                    break;
                case "2a":
                    crypto = CryptType.BCrypt;
                    break;
                case "argon2i":
                    crypto = CryptType.ARGON2I;
                    break;
                case "argon2id":
                    crypto = CryptType.ARGON2ID;
                    break;
                default:
                    crypto = CryptType.UNKNOWN;
                    break;
            }

            if (crypto == CryptType.UNKNOWN)
                switch (type_backend.toLowerCase()) {
                    case "sha512":
                    case "512":
                        crypto = CryptType.SHA512;
                        break;
                    case "sha256":
                    case "256":
                        crypto = CryptType.SHA256;
                        break;
                    case "2y":
                        crypto = CryptType.BCryptPHP;
                        break;
                    case "2a":
                        crypto = CryptType.BCrypt;
                        break;
                    case "argon2i":
                        crypto = CryptType.ARGON2I;
                        break;
                    case "argon2id":
                        crypto = CryptType.ARGON2ID;
                        break;
                    default:
                        crypto = CryptType.UNKNOWN;
                        break;
                }

            return crypto;
        } catch (Throwable ex) {
            return CryptType.UNKNOWN;
        }
    }

    /**
     * Encrypt the password
     *
     * @param type the encryption type
     * @param toBase64 convert the hashed password to
     *                 base 64
     * @return the encrypted password
     */
    public final String hash(final CryptType type, final boolean toBase64) {
        EncryptionMethod method;

        switch (type) {
            case BCrypt:
            case BCryptPHP:
            case LOGINSECURITY:
                method = new BCryptUtil(fromBase64(false), fromBase64(true), type);
                break;
            case ARGON2I:
            case ARGON2ID:
                method = new Argon2Util(fromBase64(false), fromBase64(true), type);
                break;
            case SHA256:
                method = new SHA256Util(fromBase64(false), fromBase64(true));
                break;
            case SHA512:
            case NONE:
            case UNKNOWN:
            case AUTHME:
            default:
                method = new SHA512Util(fromBase64(false), fromBase64(true));
                break;
        }

        String hashed = method.hash();

        return (toBase64 ? Base64.getEncoder().encodeToString(hashed.getBytes(StandardCharsets.UTF_8)) : hashed);
    }

    /**
     * Check if the password needs a rehash
     *
     * @param current the current crypt type
     * @return if the password needs a rehash
     */
    public final boolean needsRehash(final CryptType current) {
        CryptType token_crypto = getType();

        try {
            String unHashed_token = fromBase64(true);
            String token_salt = unHashed_token.split("\\$")[1];

            if (token_salt.length() <= 1)
                return true;
        } catch (Throwable ignored) {}

        if (!token_crypto.equals(CryptType.NONE) && !token_crypto.equals(CryptType.UNKNOWN))
            return !current.equals(token_crypto);
        else
            return true;

    }

    /**
     * Check if the password is correct
     *
     * @return if the password is valid
     */
    public final boolean validate() {
        CryptType current_type = getType();

        String key = token;
        if (isBase64(true))
            key = fromBase64(true);

        EncryptionMethod method;

        switch (current_type) {
            case SHA512:
                method = new SHA512Util(password, key);
                break;
            case SHA256:
                method = new SHA256Util(password, key);
                break;
            case BCrypt:
            case BCryptPHP:
            case LOGINSECURITY:
                method = new BCryptUtil(password, key, current_type);
                break;
            case ARGON2I:
            case ARGON2ID:
                method = new Argon2Util(password, key, current_type);
                break;
            case UNKNOWN:
            case AUTHME:
                method = new AuthMeAuth(password, key);
                break;
            case NONE:
            default:
                Console.send("&cError while getting current token hash type: {0}", current_type.name());
                return false;
        }

        return method.check();
    }

    /**
     * Change the specified token salt
     *
     * @param salt the new token salt
     * @return the updated token
     */
    public final String updateSalt(final String salt) {
        String newToken = token;
        if (isBase64(true))
            newToken = fromBase64(true);

        String[] data = newToken.split("\\$");
        String tokenSalt = "\\$" + data[1];

        return newToken.replaceFirst(tokenSalt, "\\$" + salt);
    }

    /**
     * Get the specified token salt
     *
     * @return the token salt
     */
    public final String getSalt() {
        String tokenClone = token;
        if (isBase64(true))
            tokenClone = fromBase64(true);

        String[] data = tokenClone.split("\\$");
        return data[1];
    }

    /**
     * Check if the password/token is base 64
     *
     * @param checkToken check if the token is
     *              base 64
     * @return if the password or token is base 64
     */
    private boolean isBase64(boolean checkToken) {
        if (checkToken)
            try {
                Base64.getDecoder().decode(token.getBytes(StandardCharsets.UTF_8));
                return true;
            } catch (Throwable ex) {
                return false;
            }
        else
            try {
                Base64.getDecoder().decode(password.getBytes(StandardCharsets.UTF_8));
                return true;
            } catch (Throwable ex) {
                return false;
            }
    }
}
