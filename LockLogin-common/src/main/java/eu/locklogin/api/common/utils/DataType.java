package eu.locklogin.api.common.utils;

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

/**
 * LockLogin plugin message data type
 */
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
    LOOKUPGUI,

    /**
     * LockLogin player object
     */
    PLAYER,

    /**
     * LockLogin module message
     */
    MODULE,

    /**
     * Send proxy id as owner
     */
    KEY,

    /**
     * Register proxy id
     */
    REGISTER,

    /**
     * Remove proxy id
     */
    REMOVE,
}
