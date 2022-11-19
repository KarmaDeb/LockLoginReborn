package eu.locklogin.api.account;

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

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * LockLogin client session
 */
public abstract class ClientSession implements Serializable {

    /**
     * Initialize the session
     * <p>
     * In this process, the captcha an instant
     * are generated, with boolean values ( all
     * should be false by default)
     *
     * @param sessionId the session id
     */
    public abstract void initialize(final AccountID sessionId);

    /**
     * Validate the session
     */
    public abstract void validate();

    /**
     * Invalidate the session
     */
    public abstract void invalidate();

    /**
     * Get the session id
     *
     * @return the session id
     */
    public abstract AccountID getName();

    /**
     * Get the session initialization
     *
     * @return the session initialization time
     */
    public abstract Instant getInitialization();

    /**
     * Check if the session has been bungee-verified
     *
     * @return if the session has been verified by
     * bungeecord
     */
    public abstract boolean isValid();

    /**
     * Check if the session is captcha-logged
     *
     * @return if the session is captcha-logged
     */
    public abstract boolean isCaptchaLogged();

    /**
     * Set the session captcha log status
     *
     * @param status the captcha log status
     */
    public abstract void setCaptchaLogged(final boolean status);

    /**
     * Check if the session is logged
     *
     * @return if the session is logged
     */
    public abstract boolean isLogged();

    /**
     * Set the session log status
     *
     * @param status the session login status
     */
    public abstract void setLogged(final boolean status);

    /**
     * Check if the session is temp logged
     *
     * @return if the session is temp logged
     */
    public abstract boolean isTempLogged();

    /**
     * Check if the session is 2fa logged
     *
     * @return if the session is 2fa logged
     */
    public abstract boolean is2FALogged();

    /**
     * Set the session 2fa log status
     *
     * @param status the session 2fa log status
     */
    public abstract void set2FALogged(final boolean status);

    /**
     * Check if the session is pin logged
     *
     * @return if the session is pin logged
     */
    public abstract boolean isPinLogged();

    /**
     * Set the session pin log status
     *
     * @param status the session pin log status
     */
    public abstract void setPinLogged(final boolean status);

    /**
     * Get the session captcha
     *
     * @return the session captcha
     */
    public abstract String getCaptcha();

    public static final AccountID DEFAULT = AccountID.fromUUID(UUID.nameUUIDFromBytes(("Default").getBytes()));
}
