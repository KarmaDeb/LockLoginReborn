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

/**
 * LockLogin client session
 */
public abstract class ClientSession implements Serializable {

    protected final AccountID id;

    /**
     * Initialize the client session
     *
     * @param account the account id
     */
    public ClientSession(final AccountID account) {
        id = account;
    }

    /**
     * Get the session ID
     *
     * @return the session ID
     */
    public final AccountID id() {
        return id;
    }

    /**
     * Initialize the session
     * <p>
     * In this process, the captcha an instant
     * are generated, with boolean values ( all
     * should be false by default)
     */
    public abstract void initialize();

    /**
     * Validate the session
     * @throws SecurityException if unauthorized access
     */
    public abstract void validate() throws SecurityException;

    /**
     * Invalidate the session
     * @throws SecurityException if unauthorized access
     */
    public abstract void invalidate() throws SecurityException;

    /**
     * Get the session initialization
     *
     * @return the session initialization time
     * @throws SecurityException if unauthorized access
     */
    @SuppressWarnings("unused")
    public abstract Instant getInitialization() throws SecurityException;

    /**
     * Check if the session has been bungee-verified
     *
     * @return if the session has been verified by
     * bungeecord
     * @throws SecurityException if unauthorized access
     */
    public abstract boolean isValid() throws SecurityException;

    /**
     * Check if the session is captcha-logged
     *
     * @return if the session is captcha-logged
     * @throws SecurityException if unauthorized access
     */
    public abstract boolean isCaptchaLogged() throws SecurityException;

    /**
     * Set the session captcha log status
     *
     * @param status the captcha log status
     * @throws SecurityException if unauthorized access
     */
    public abstract void setCaptchaLogged(final boolean status) throws SecurityException;

    /**
     * Check if the session is logged
     *
     * @return if the session is logged
     * @throws SecurityException if unauthorized access
     */
    public abstract boolean isLogged() throws SecurityException;

    /**
     * Set the session log status
     *
     * @param status the session login status
     * @throws SecurityException if unauthorized access
     */
    public abstract void setLogged(final boolean status) throws SecurityException;

    /**
     * Check if the session is temp logged
     *
     * @return if the session is temp logged
     * @throws SecurityException if unauthorized access
     */
    public abstract boolean isTempLogged() throws SecurityException;

    /**
     * Check if the session is 2fa logged
     *
     * @return if the session is 2fa logged
     * @throws SecurityException if unauthorized access
     */
    public abstract boolean is2FALogged() throws SecurityException;

    /**
     * Set the session 2fa log status
     *
     * @param status the session 2fa log status
     * @throws SecurityException if unauthorized access
     */
    public abstract void set2FALogged(final boolean status) throws SecurityException;

    /**
     * Check if the session is pin logged
     *
     * @return if the session is pin logged
     * @throws SecurityException if unauthorized access
     */
    public abstract boolean isPinLogged() throws SecurityException;

    /**
     * Set the session pin log status
     *
     * @param status the session pin log status
     * @throws SecurityException if unauthorized access
     */
    public abstract void setPinLogged(final boolean status) throws SecurityException;

    /**
     * Get the session captcha
     *
     * @return the session captcha
     * @throws SecurityException if unauthorized access
     */
    public abstract String getCaptcha() throws SecurityException;
}
