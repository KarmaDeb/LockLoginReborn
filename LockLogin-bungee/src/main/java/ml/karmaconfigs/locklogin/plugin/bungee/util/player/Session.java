package ml.karmaconfigs.locklogin.plugin.bungee.util.player;

import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;

import java.time.Instant;

public final class Session extends ClientSession {

    private Instant initialized;

    private String captcha = "";

    private boolean captcha_logged = false;
    private boolean logged = false;
    private boolean pin_logged = false;
    private boolean gAuth_logged = false;
    private boolean bungee_verified = true;

    private Session() {
    }

    /**
     * Initialize the session
     */
    @Override
    public final void initialize() {
        initialized = Instant.now();

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        if (captcha.isEmpty() || !captcha_logged) {
            StringUtils.StringGen generation = StringUtils.StringGen.ONLY_NUMBERS;
            StringUtils.StringType type = StringUtils.StringType.RANDOM_SIZE;

            if (config.captchaOptions().hasLetters()) {
                generation = StringUtils.StringGen.NUMBERS_AND_LETTERS;
            }

            captcha = StringUtils.randomString(config.captchaOptions().getLength(), generation, type);
        }
    }

    /**
     * Validate the session
     */
    @Override
    public final void validate() {
        bungee_verified = true;
    }

    /**
     * Invalidate the session
     */
    @Override
    public final void invalidate() {
        bungee_verified = false;
    }

    /**
     * Get the session initialization
     *
     * @return the session initialization time
     */
    @Override
    public final Instant getInitialization() {
        return initialized;
    }

    /**
     * Check if the session has been bungee-verified
     *
     * @return if the session has been verified by
     * bungeecord
     */
    @Override
    public final boolean isValid() {
        return bungee_verified;
    }

    /**
     * Check if the session is captcha-logged
     *
     * @return if the session is captcha-logged
     */
    @Override
    public final boolean isCaptchaLogged() {
        return captcha_logged;
    }

    /**
     * Set the session captcha log status
     *
     * @param status the captcha log status
     */
    @Override
    public final void setCaptchaLogged(final boolean status) {
        captcha_logged = status;
    }

    /**
     * Check if the session is logged
     *
     * @return if the session is logged
     */
    @Override
    public final boolean isLogged() {
        return logged;
    }

    /**
     * Set the session log status
     *
     * @param status the session login status
     */
    @Override
    public final void setLogged(final boolean status) {
        logged = status;
    }

    /**
     * Check if the session is temp logged
     *
     * @return if the session is temp logged
     */
    @Override
    public final boolean isTempLogged() {
        return gAuth_logged && pin_logged;
    }

    /**
     * Check if the session is 2fa logged
     *
     * @return if the session is 2fa logged
     */
    @Override
    public boolean is2FALogged() {
        return gAuth_logged;
    }

    /**
     * Set the session 2fa log status
     *
     * @param status the session 2fa log status
     */
    @Override
    public final void set2FALogged(final boolean status) {
        gAuth_logged = status;
    }

    /**
     * Check if the session is pin logged
     *
     * @return if the session is pin logged
     */
    @Override
    public boolean isPinLogged() {
        return pin_logged;
    }

    /**
     * Set the session pin log status
     *
     * @param status the session pin log status
     */
    @Override
    public final void setPinLogged(final boolean status) {
        pin_logged = status;
    }

    /**
     * Get the session captcha
     *
     * @return the session captcha
     */
    @Override
    public final String getCaptcha() {
        return captcha;
    }
}
