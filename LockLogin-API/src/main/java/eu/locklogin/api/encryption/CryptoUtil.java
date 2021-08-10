package eu.locklogin.api.encryption;

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

import eu.locklogin.api.encryption.argon.Argon2Util;
import eu.locklogin.api.encryption.libraries.bcrypt.BCryptLib;
import eu.locklogin.api.encryption.libraries.sha.LSSHA256;
import eu.locklogin.api.encryption.libraries.sha.SHA256;
import eu.locklogin.api.encryption.libraries.sha.SHA512;
import eu.locklogin.api.encryption.libraries.sha.SHA512X;
import eu.locklogin.api.encryption.plugin.AuthMeAuth;
import eu.locklogin.api.encryption.plugin.LoginSecurityAuth;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * LockLogin crypto util
 */
public final class CryptoUtil {

    private final String password;
    private final String token;

    /**
     * Initialize the crypto util instance
     *
     * @param pwd the password
     * @param tkn the token
     */
    CryptoUtil(final String pwd, final String tkn) {
        password = pwd;
        token = tkn;
    }

    /**
     * Get the crypto util builder
     *
     * @return a new crypto util builder
     */
    public static Builder getBuilder() {
        return new Builder();
    }

    /**
     * Encrypt the password/token to base 64
     *
     * @param target the crypt target
     * @return the encrypted target
     */
    public final String toBase64(final CryptTarget target) {
        if (!isBase64(target)) {
            switch (target) {
                case PASSWORD:
                    if (!StringUtils.isNullOrEmpty(password)) {
                        return Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
                    }

                    return "";
                case TOKEN:
                    if (!StringUtils.isNullOrEmpty(token)) {
                        return Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
                    }

                    return "";
                default:
                    return "";
            }
        }

        return (target.equals(CryptTarget.PASSWORD) ? password : token);
    }

    /**
     * Un-encrypt the password/token from base 64
     *
     * @param target the crypt target
     * @return the un-encrypted target
     */
    public final String fromBase64(final CryptTarget target) {
        if (isBase64(target)) {
            switch (target) {
                case PASSWORD:
                    if (!StringUtils.isNullOrEmpty(password)) {
                        return new String(Base64.getDecoder().decode(password.getBytes(StandardCharsets.UTF_8)));
                    }

                    return "";
                case TOKEN:
                    if (!StringUtils.isNullOrEmpty(token)) {
                        return new String(Base64.getDecoder().decode(token.getBytes(StandardCharsets.UTF_8)));
                    }

                    return "";
                default:
                    return "";
            }
        }

        return (target.equals(CryptTarget.PASSWORD) ? password : token);
    }

    /**
     * Hash the password
     *
     * @param type the password hashing type
     * @param encrypt encrypt the hash result to base 64
     * @return the hashed password
     */
    public final String hash(final HashType type, final boolean encrypt) throws IllegalArgumentException {
        if (!StringUtils.isNullOrEmpty(password)) {
            switch (type) {
                case SHA512:
                    SHA512 sha512 = new SHA512();
                    if (encrypt) {
                        return Base64.getEncoder().encodeToString(sha512.hash(password).getBytes(StandardCharsets.UTF_8));
                    } else {
                        return sha512.hash(password);
                    }
                case SHA256:
                    SHA256 sha256 = new SHA256(password);
                    if (encrypt) {
                        return Base64.getEncoder().encodeToString(sha256.hash().getBytes(StandardCharsets.UTF_8));
                    } else {
                        return sha256.hash();
                    }
                case LS_SHA256:
                    LSSHA256 lessSHA256 = new LSSHA256(password);
                    if (encrypt) {
                        return Base64.getEncoder().encodeToString(lessSHA256.hash().getBytes(StandardCharsets.UTF_8));
                    } else {
                        return lessSHA256.hash();
                    }
                case BCrypt:
                case BCryptPHP:
                    if (encrypt) {
                        return Base64.getEncoder().encodeToString(BCryptLib.hashpw(password, BCryptLib.gensalt()).getBytes(StandardCharsets.UTF_8));
                    } else {
                        return BCryptLib.hashpw(password, BCryptLib.gensalt());
                    }
                case ARGON2I:
                case ARGON2ID:
                    Argon2Util argon2 = new Argon2Util(password);
                    if (encrypt) {
                        return Base64.getEncoder().encodeToString(argon2.hashPassword(type).getBytes(StandardCharsets.UTF_8));
                    } else {
                        return argon2.hashPassword(type);
                    }
                case NONE:
                case UNKNOWN:
                default:
                    throw new IllegalArgumentException("Hash type can't be none, unknown or non-defined! ( " + type.name() + " )");
            }
        }

        return (!StringUtils.isNullOrEmpty(token) ? token : "");
    }

    /**
     * Get the token hash type
     *
     * @return the token hash type
     */
    public final HashType getTokenHash() {
        if (!StringUtils.isNullOrEmpty(token)) {
            String clean = fromBase64(CryptTarget.TOKEN);

            try {
                String[] data = clean.split("\\$");

                String type = data[1];

                switch (type.toLowerCase()) {
                    case "sha512":
                    case "512":
                        return HashType.SHA512;
                    case "sha256":
                    case "256":
                        return HashType.SHA256;
                    case "2y":
                        return HashType.BCryptPHP;
                    case "2a":
                        return HashType.BCrypt;
                    case "argon2i":
                        return HashType.ARGON2I;
                    case "argon2id":
                        return HashType.ARGON2ID;
                    default:
                        type = data[2];
                        switch (type) {
                            case "sha512":
                            case "512":
                                return HashType.SHA512;
                            case "sha256":
                            case "256":
                                return HashType.SHA256;
                            case "2y":
                                return HashType.BCryptPHP;
                            case "2a":
                                return HashType.BCrypt;
                            case "argon2i":
                                return HashType.ARGON2I;
                            case "argon2id":
                                return HashType.ARGON2ID;
                            default:
                                return HashType.UNKNOWN;
                        }
                }
            } catch (Throwable ex) {
                if (clean.getBytes().length == 64) {
                    return HashType.LS_SHA256;
                } else {
                    return HashType.UNKNOWN;
                }
            }
        }

        return HashType.NONE;
    }

    /**
     * Check if the current token needs a re-hash
     *
     * @param current_crypto the current crypt type
     * @return if the token needs a re-hash
     */
    public final boolean needsRehash(final HashType current_crypto) {
        try {
            String clean = fromBase64(CryptTarget.TOKEN);
            String token_salt = clean.split("\\$")[1];
            if (token_salt.length() <= 1)
                return true;
        } catch (Throwable ex) {
            return true;
        }

        HashType token_crypto = getTokenHash();

        if (!token_crypto.equals(HashType.NONE) && !token_crypto.equals(HashType.UNKNOWN))
            return !current_crypto.equals(token_crypto);
        else
            return true;
    }

    /**
     * Check if the password is equals
     * to the decoded password
     *
     * @return if the password is correct
     */
    public final boolean validate() {
        if (!StringUtils.isNullOrEmpty(token) && !StringUtils.isNullOrEmpty(password)) {
            HashType current_type = getTokenHash();

            String key = token;
            if (isBase64(key))
                key = new String(Base64.getDecoder().decode(key));

            SHA512 sha512 = new SHA512();
            SHA512X sha512X = new SHA512X(password);
            Argon2Util argon2id = new Argon2Util(password);

            SaltData data = new SaltData(key);
            switch (current_type) {
                case SHA512:
                    return sha512.auth(password, key);
                case SHA256:
                    SHA256 sha256 = new SHA256(password);
                    return sha256.check(key);
                case LS_SHA256:
                    LSSHA256 lssha256 = new LSSHA256(password);
                    return lssha256.check(key);
                case BCrypt:
                case BCryptPHP:
                    return BCryptLib.checkpw(password, key.replaceFirst("2y", "2a"));
                case ARGON2I:
                    return argon2id.checkPassword(key, HashType.ARGON2I);
                case ARGON2ID:
                    return argon2id.checkPassword(key, HashType.ARGON2ID);
                case UNKNOWN:
                    return AuthMeAuth.check(password, key) || LoginSecurityAuth.check(password, key) || sha512X.validate(key, data.getSalt());
                case NONE:
                default:
                    Console.send("&cError while getting current token hash type: " + current_type.name());
                    return false;
            }
        }

        return false;
    }

    /**
     * Get if the password or token is base 64
     *
     * @param target the check target
     * @return if the target is base 64
     */
    public final boolean isBase64(final CryptTarget target) {
        switch (target) {
            case PASSWORD:
                if (!StringUtils.isNullOrEmpty(password)) {
                    return isBase64(password);
                }

                return false;
            case TOKEN:
                if (!StringUtils.isNullOrEmpty(token)) {
                    return isBase64(token);
                }

                return false;
            default:
                //As we always return "" for default value, true is
                //our fail-true state
                return true;
        }
    }

    /**
     * Get the crypto util builder
     */
    public static class Builder {

        private String password = "";
        private String token = "";

        /**
         * Build with password
         *
         * @param pwd the password
         * @return this instance
         */
        public Builder withPassword(final Object pwd) {
            if (pwd != null) {
                password = pwd.toString();
            }
            return this;
        }

        /**
         * Build with token
         *
         * @param tkn the token
         * @return this instance
         */
        public Builder withToken(final Object tkn) {
            if (tkn != null) {
                token = tkn.toString();
            }
            return this;
        }

        /**
         * Build the builder
         *
         * @return the crypto util instance
         */
        public CryptoUtil build() throws IllegalArgumentException {
            if (password.replaceAll("\\s", "").isEmpty() && token.replaceAll("\\s", "").isEmpty()) {
                throw new IllegalArgumentException("Tried to build a crypto util instance with empty/null password and token");
            } else {
                return new CryptoUtil(password, token);
            }
        }

        /**
         * Build the builder unsafely
         *
         * @return the crypto util instance
         */
        public CryptoUtil unsafe() {
            return new CryptoUtil(password, token);
        }
    }

    /**
     * Get if the string is base 64
     *
     * @param data the string
     * @return if the string is a base64 string
     */
    private boolean isBase64(final String data){
        String regex =
                "([A-Za-z0-9+/]{4})*"+
                        "([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)";

        Pattern patron = Pattern.compile(regex);
        return patron.matcher(data).matches();
    }
}
