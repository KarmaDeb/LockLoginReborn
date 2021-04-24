package ml.karmaconfigs.locklogin.api.encryption;

public enum CryptType {
    /**
     * LockLogin compatible encryption type
     */
    SHA256,
    /**
     * LockLogin compatible encryption type
     */
    SHA512,
    /**
     * LockLogin compatible encryption type
     */
    BCrypt,
    /**
     * LockLogin compatible encryption type
     */
    BCryptPHP,
    /**
     * LockLogin compatible encryption type
     */
    ARGON2I,
    /**
     * LockLogin compatible encryption type
     */
    ARGON2ID,
    /**
     * AuthMe default encryption type ( double SHA256 )
     */
    AUTHME,
    /**
     * Login security encryption type ( same as BCrypt )
     */
    LOGINSECURITY,
    /**
     * LockLogin unknown encryption type
     */
    UNKNOWN,
    /**
     * No encryption type
     */
    NONE
}
