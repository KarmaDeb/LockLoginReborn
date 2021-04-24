package ml.karmaconfigs.locklogin.api.account;

import java.time.Instant;

public abstract class ClientSession {

    /**
     * Initialize the session
     *
     * In this process the captcha and instant
     * are generated, with boolean values ( all
     * should be false by default )
     */
    public abstract void initialize();

    /**
     * Set the session captcha log status
     *
     * @param status the captcha log status
     */
    public abstract void setCaptchaLogged(final boolean status);

    /**
     * Set the session log status
     *
     * @param status the session login status
     */
    public abstract void setLogged(final boolean status);

    /**
     * Set the session temp log status
     *
     * @param status the session temp log status
     */
    public abstract void setTempLogged(final boolean status);

    /**
     * Validate the session
     */
    public abstract void validate();

    /**
     * Invalidate the session
     */
    public abstract void invalidate();

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
     * Check if the session is logged
     *
     * @return if the session is logged
     */
    public abstract boolean isLogged();

    /**
     * Check if the session is temp logged
     *
     * @return if the session is temp logged
     */
    public abstract boolean isTempLogged();


    /**
     * Get the session captcha
     *
     * @return the session captcha
     */
    public abstract String getCaptcha();
}
