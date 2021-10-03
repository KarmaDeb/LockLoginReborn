package eu.locklogin.api.common.session;

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

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.utils.StringUtils;

import java.time.Instant;

/**
 * LockLogin default session manager
 */
public final class Session extends ClientSession {

    private Instant initialized;

    private String captcha = "";

    private boolean captcha_logged = false;
    private boolean logged = false;
    private boolean pin_logged = false;
    private boolean gAuth_logged = false;
    private boolean bungee_verified = true;

    private Session() {}

    /**
     * Initialize the session
     */
    @Override
    public void initialize() {
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
    public void validate() {
        bungee_verified = true;
    }

    /**
     * Invalidate the session
     */
    @Override
    public void invalidate() {
        bungee_verified = false;
    }

    /**
     * Get the session initialization
     *
     * @return the session initialization time
     */
    @Override
    public Instant getInitialization() {
        return initialized;
    }

    /**
     * Check if the session has been bungee-verified
     *
     * @return if the session has been verified by
     * bungeecord
     */
    @Override
    public boolean isValid() {
        return bungee_verified;
    }

    /**
     * Check if the session is captcha-logged
     *
     * @return if the session is captcha-logged
     */
    @Override
    public boolean isCaptchaLogged() {
        return captcha_logged;
    }

    /**
     * Set the session captcha log status
     *
     * @param status the captcha log status
     */
    @Override
    public void setCaptchaLogged(final boolean status) {
        captcha_logged = status;
    }

    /**
     * Check if the session is logged
     *
     * @return if the session is logged
     */
    @Override
    public boolean isLogged() {
        return logged;
    }

    /**
     * Set the session log status
     *
     * @param status the session login status
     */
    @Override
    public void setLogged(final boolean status) {
        logged = status;
    }

    /**
     * Check if the session is temp logged
     *
     * @return if the session is temp logged
     */
    @Override
    public boolean isTempLogged() {
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
    public void set2FALogged(final boolean status) {
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
    public void setPinLogged(final boolean status) {
        pin_logged = status;
    }

    /**
     * Get the session captcha
     *
     * @return the session captcha
     */
    @Override
    public String getCaptcha() {
        return captcha;
    }
}

