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
import eu.locklogin.api.encryption.libraries.wordpress.WordPressCrypt;
import eu.locklogin.api.encryption.plugin.AuthMeAuth;
import eu.locklogin.api.encryption.plugin.LoginSecurityAuth;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaElement;
import ml.karmaconfigs.api.common.karma.file.element.KarmaObject;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * LockLogin crypto util
 */
public final class CryptoFactory {

    private static String virtual_id = "";

    private final String password;
    private final String token;

    /**
     * Initialize the crypto util instance
     *
     * @param pwd the password
     * @param tkn the token
     */
    CryptoFactory(final String pwd, final String tkn) {
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
    public String toBase64(final CryptTarget target) {
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
    public String fromBase64(final CryptTarget target) {
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
     * @param type    the password hashing type
     * @param encrypt encrypt the hash result to base 64
     * @return the hashed password
     */
    public String hash(final HashType type, final boolean encrypt) throws IllegalArgumentException {
        if (!StringUtils.isNullOrEmpty(password)) {
            switch (type) {
                case SHA512:
                    SHA512 sha512 = new SHA512();
                    if (encrypt) {
                        return Base64.getEncoder().encodeToString(sha512.hash(virtual_id + password).getBytes(StandardCharsets.UTF_8));
                    } else {
                        return sha512.hash(virtual_id + password);
                    }
                case SHA256:
                    SHA256 sha256 = new SHA256(virtual_id + password);
                    if (encrypt) {
                        return Base64.getEncoder().encodeToString(sha256.hash().getBytes(StandardCharsets.UTF_8));
                    } else {
                        return sha256.hash();
                    }
                case LS_SHA256:
                    LSSHA256 lessSHA256 = new LSSHA256(virtual_id + password);
                    if (encrypt) {
                        return Base64.getEncoder().encodeToString(lessSHA256.hash().getBytes(StandardCharsets.UTF_8));
                    } else {
                        return lessSHA256.hash();
                    }
                case BCrypt:
                case BCryptPHP:
                    if (encrypt) {
                        return Base64.getEncoder().encodeToString(BCryptLib.hashpw(virtual_id + password, BCryptLib.gensalt()).getBytes(StandardCharsets.UTF_8));
                    } else {
                        return BCryptLib.hashpw(virtual_id + password, BCryptLib.gensalt());
                    }
                case ARGON2I:
                case ARGON2ID:
                    Argon2Util argon2 = new Argon2Util(virtual_id + password);
                    if (encrypt) {
                        return Base64.getEncoder().encodeToString(argon2.hashPassword(type).getBytes(StandardCharsets.UTF_8));
                    } else {
                        return argon2.hashPassword(type);
                    }
                case AUTHME_SHA:
                    if (encrypt) {
                        return Base64.getEncoder().encodeToString(AuthMeAuth.hashSha256(virtual_id + password).getBytes(StandardCharsets.UTF_8));
                    } else {
                        return AuthMeAuth.hashSha256(virtual_id + password);
                    }
                case WORDPRESS:
                    WordPressCrypt crypt = new WordPressCrypt(virtual_id + password);
                    if (encrypt) {
                        return Base64.getEncoder().encodeToString(crypt.hash().getBytes(StandardCharsets.UTF_8));
                    } else {
                        return crypt.hash();
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
    public HashType getTokenHash() {
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
                    case "sha":
                        return HashType.AUTHME_SHA;
                    case "p":
                    case "h":
                        return HashType.WORDPRESS;
                    default:
                        type = data[2];
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
                            case "sha":
                                return HashType.AUTHME_SHA;
                            case "p":
                            case "h":
                                return HashType.WORDPRESS;
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
    public boolean needsRehash(final HashType current_crypto) {
        if (!StringUtils.isNullOrEmpty(password) && !StringUtils.isNullOrEmpty(token)) {
            if (!validate(Validation.MODERN) && validate(Validation.LEGACY)) {
                //Basically, we can authenticate the user using legacy password verification ( without virtual id ). But we cannot
                //validate him with the modern password verification ( virtual id based )
                return true;
            }
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
     * @param validation set the validation method
     * @return if the password is correct
     */
    public boolean validate(final Validation validation) {
        if (!StringUtils.isNullOrEmpty(token) && !StringUtils.isNullOrEmpty(password)) {
            HashType current_type = getTokenHash();

            String key = token;
            if (isBase64(key))
                key = new String(Base64.getDecoder().decode(key));

            SHA512 sha512 = new SHA512();

            String pwd;
            switch (validation) {
                case ALL:
                    String password_try = virtual_id + password;
                    boolean try_result = false;

                    SHA512X sha512X = new SHA512X(password_try);
                    Argon2Util argon2id = new Argon2Util(password_try);
                    WordPressCrypt wordPress = new WordPressCrypt(password_try);

                    SaltData data = new SaltData(key);
                    switch (current_type) {
                        case SHA512:
                            try_result = sha512.auth(password_try, key);
                            break;
                        case SHA256:
                            SHA256 sha256 = new SHA256(password_try);
                            try_result = sha256.check(key);
                            break;
                        case LS_SHA256:
                            LSSHA256 lssha256 = new LSSHA256(password_try);
                            try_result = lssha256.check(key);
                            break;
                        case BCrypt:
                        case BCryptPHP:
                            try_result = BCryptLib.checkpw(password_try, key.replaceFirst("2y", "2a"));
                            break;
                        case ARGON2I:
                            try_result = argon2id.checkPassword(key, HashType.ARGON2I);
                            break;
                        case ARGON2ID:
                            try_result = argon2id.checkPassword(key, HashType.ARGON2ID);
                            break;
                        case AUTHME_SHA:
                            try_result = AuthMeAuth.check(password_try, key);
                            break;
                        case WORDPRESS:
                            try_result = wordPress.validate(key);
                            break;
                        case UNKNOWN:
                            try_result = AuthMeAuth.check(password_try, key) || LoginSecurityAuth.check(password_try, key) || sha512X.validate(key, data.getSalt());
                            break;
                        case NONE:
                        default:
                            APISource.loadProvider("LockLogin").console().send("&cError while getting current token hash type: " + current_type.name());
                            break;
                    }

                    if (!try_result) {
                        password_try = password;

                        sha512X = new SHA512X(password_try);
                        argon2id = new Argon2Util(password_try);
                        wordPress = new WordPressCrypt(password_try);

                        data = new SaltData(key);
                        switch (current_type) {
                            case SHA512:
                                try_result = sha512.auth(password_try, key);
                                break;
                            case SHA256:
                                SHA256 sha256 = new SHA256(password_try);
                                try_result = sha256.check(key);
                                break;
                            case LS_SHA256:
                                LSSHA256 lssha256 = new LSSHA256(password_try);
                                try_result = lssha256.check(key);
                                break;
                            case BCrypt:
                            case BCryptPHP:
                                try_result = BCryptLib.checkpw(password_try, key.replaceFirst("2y", "2a"));
                                break;
                            case ARGON2I:
                                try_result = argon2id.checkPassword(key, HashType.ARGON2I);
                                break;
                            case ARGON2ID:
                                try_result = argon2id.checkPassword(key, HashType.ARGON2ID);
                                break;
                            case AUTHME_SHA:
                                try_result = AuthMeAuth.check(password_try, key);
                                break;
                            case WORDPRESS:
                                try_result = wordPress.validate(key);
                                break;
                            case UNKNOWN:
                                try_result = AuthMeAuth.check(password_try, key) || LoginSecurityAuth.check(password_try, key) || sha512X.validate(key, data.getSalt());
                                break;
                            case NONE:
                            default:
                                APISource.loadProvider("LockLogin").console().send("&cError while getting current token hash type: " + current_type.name());
                                break;
                        }
                    }

                    return try_result;
                case LEGACY:
                    pwd = password;
                    break;
                case MODERN:
                default:
                    pwd = virtual_id + password;
                    break;
            }

            String usePassword = pwd;

            SHA512X sha512X = new SHA512X(usePassword);
            Argon2Util argon2id = new Argon2Util(usePassword);
            WordPressCrypt wordPress = new WordPressCrypt(usePassword);

            SaltData data = new SaltData(key);
            switch (current_type) {
                case SHA512:
                    return sha512.auth(usePassword, key);
                case SHA256:
                    SHA256 sha256 = new SHA256(usePassword);
                    return sha256.check(key);
                case LS_SHA256:
                    LSSHA256 lssha256 = new LSSHA256(usePassword);
                    return lssha256.check(key);
                case BCrypt:
                case BCryptPHP:
                    return BCryptLib.checkpw(usePassword, key.replaceFirst("2y", "2a"));
                case ARGON2I:
                    return argon2id.checkPassword(key, HashType.ARGON2I);
                case ARGON2ID:
                    return argon2id.checkPassword(key, HashType.ARGON2ID);
                case AUTHME_SHA:
                    return AuthMeAuth.check(usePassword, key);
                case WORDPRESS:
                    return wordPress.validate(key);
                case UNKNOWN:
                    return AuthMeAuth.check(usePassword, key) || LoginSecurityAuth.check(usePassword, key) || sha512X.validate(key, data.getSalt());
                case NONE:
                default:
                    APISource.loadProvider("LockLogin").console().send("&cError while getting current token hash type: " + current_type.name());
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
    public boolean isBase64(final CryptTarget target) {
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
     * Get if the string is base 64
     *
     * @param data the string
     * @return if the string is a base64 string
     */
    private boolean isBase64(final String data) {
        String regex =
                "([A-Za-z0-9+/]{4})*" +
                        "([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)";

        Pattern patron = Pattern.compile(regex);
        return patron.matcher(data).matches();
    }

    /**
     * Load the plugin virtual key used on password hashing
     * to avoid password cracks on database leaks
     */
    public static void loadVirtualID() {
        KarmaSource plugin = APISource.loadProvider("LockLogin");
        KarmaMain vid = new KarmaMain(plugin, "virtual_id.kf", "cache");

        /*
        boolean proceed;
        FileEncryptor e = new FileEncryptor(vid.getDocument(), CurrentPlatform.getServerHash());

        There's no purpose on encrypting a file that is being encrypted with a password that is not secret.

        virtual id will help to avoid database leaks for systems such as MySQL. But there's no guarantee that file
        system will be completely protected using this system as to steal player file accounts you need access to the
        machine, so you also have access to the virtual id file.
         */

        /*if (e.decrypt()) {
            proceed = true;
        } else {
            try {
                KarmaSource original = APISource.getOriginal(true);
                Throwable error = original.getHandler(FileEncryptor.class).getLast();
                if (error != null)
                    error.printStackTrace();
            } catch (Throwable ignored) {}

            if (vid.exists()) {
                proceed = vid.isSet("virtual_key");
            } else {
                proceed = true;
            }
        }

        if (proceed) {*/
            KarmaElement virtual_id = vid.get("virtual_key");
            if (virtual_id == null || !virtual_id.isString()) {
                virtual_id = new KarmaObject(TokenGenerator.generateToken());

                vid.set("virtual_key", virtual_id);
                vid.save();
            }

            if (CurrentPlatform.getConfiguration().useVirtualID()) {
                CryptoFactory.virtual_id = virtual_id.getObjet().getString();
            }
        /*} else {
            plugin.console().send("Failed to load virtual ID, try restarting your server. If the issue persists ask for support at https://discord.gg/jRFfsdxnJR", Level.GRAVE);
        }*/
    }

    /**
     * Get the crypto util builder
     */
    public final static class Builder {

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
        public CryptoFactory build() throws IllegalArgumentException {
            if (password.replaceAll("\\s", "").isEmpty() && token.replaceAll("\\s", "").isEmpty()) {
                throw new IllegalArgumentException("Tried to build a crypto util instance with empty/null password and token");
            } else {
                return new CryptoFactory(password, token);
            }
        }

        /**
         * Build the builder unsafely
         *
         * @return the crypto util instance
         */
        public CryptoFactory unsafe() {
            return new CryptoFactory(password, token);
        }
    }
}
