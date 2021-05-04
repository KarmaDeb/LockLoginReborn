package ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender;

public enum DataType {
    /**
     * Join data
     */
    JOIN,

    /**
     * Quit data
     */
    QUIT,

    /**
     * Session validation data
     */
    VALIDATION,

    /**
     * Captcha data
     */
    CAPTCHA,

    /**
     * Session data
     */
    SESSION,

    /**
     * Pin data
     */
    PIN,

    /**
     * 2FA data
     */
    GAUTH,

    /**
     * Session close data
     */
    CLOSE,

    /**
     * Potion effects data
     */
    EFFECTS,

    /**
     * Session invalidation data
     */
    INVALIDATION,

    /**
     * BungeeCord messages data
     */
    MESSAGES,

    /**
     * BungeeCord config data
     */
    CONFIG,

    /**
     * Logged sessions amount
     */
    LOGGED,

    /**
     * Registered sessions amount
     */
    REGISTERED,

    /**
     * Player info request
     */
    INFOGUI,

    /**
     * Lookup info request
     */
    LOOKUPGUI;
}
