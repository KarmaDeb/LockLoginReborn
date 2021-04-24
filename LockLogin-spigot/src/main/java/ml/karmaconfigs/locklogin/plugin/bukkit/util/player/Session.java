package ml.karmaconfigs.locklogin.plugin.bukkit.util.player;

import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.Config;

import java.time.Instant;

public final class Session extends ClientSession {

    private Instant initialized;

    private String captcha = "";

    private boolean captcha_logged = false;
    private boolean logged = false;
    private boolean temp_logged = false;
    private boolean bungee_verified = true;

    private Session() {}

    /**
     * Initialize the session
     */
    @Override
    public final void initialize() {
        initialized = Instant.now();

        Config config = new Config();
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
     * Set the session captcha log status
     *
     * @param status the captcha log status
     */
    @Override
    public final void setCaptchaLogged(final boolean status) {
        captcha_logged = status;
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
     * Set the session temp log status
     *
     * @param status the session temp log status
     */
    @Override
    public final void setTempLogged(final boolean status) {
        temp_logged = status;
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
     * Check if the session is logged
     *
     * @return if the session is logged
     */
    @Override
    public final boolean isLogged() {
        return logged;
    }

    /**
     * Check if the session is temp logged
     *
     * @return if the session is temp logged
     */
    @Override
    public final boolean isTempLogged() {
        return temp_logged;
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
