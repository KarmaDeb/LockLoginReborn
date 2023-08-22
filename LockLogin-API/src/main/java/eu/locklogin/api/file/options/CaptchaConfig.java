package eu.locklogin.api.file.options;

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
 * Captcha configuration
 */
public final class CaptchaConfig {

    private final boolean enabled;
    private final CaptchaLocation location;
    private final int length;
    private final boolean letters;
    private final boolean strikethrough;
    private final boolean randomStrikethrough;

    /**
     * Initialize the captcha configuration
     *
     * @param toggle         the captcha mode
     * @param position       the captcha position
     * @param captchaLength  the captcha code length
     * @param includeLetters include letters in code
     * @param enableStrike   enable strikethrough effect on captcha
     * @param randomStrike   randomize characters with strike effect
     */
    public CaptchaConfig(final boolean toggle, final CaptchaLocation position, final int captchaLength, final boolean includeLetters, final boolean enableStrike, final boolean randomStrike) {
        enabled = toggle;
        location = position;
        length = captchaLength;
        letters = includeLetters;
        strikethrough = enableStrike;
        randomStrikethrough = randomStrike;
    }

    /**
     * Get the captcha mode
     *
     * @return the captcha mode
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the captcha location
     *
     * @return the captcha location
     */
    public CaptchaLocation getLocation() {
        return location;
    }

    /**
     * Get the captcha code length
     *
     * @return the captcha code length
     */
    public int getLength() {
        return length;
    }

    /**
     * Get if the captcha has letters
     *
     * @return if the captcha has letters
     */
    public boolean hasLetters() {
        return letters;
    }

    /**
     * Get if the captcha has strike effect
     * enabled
     *
     * @return if the captcha has strike
     */
    public boolean enableStrike() {
        return strikethrough;
    }

    /**
     * Get if the captcha should randomize
     * the characters that get strike
     * effect
     *
     * @return if the strike effect is random
     */
    public boolean randomStrike() {
        return randomStrikethrough;
    }

    /**
     * Captcha possible locations
     */
    public enum CaptchaLocation {
        CHAT,
        ACTIONBAR,
        TITLE,
        BOSS
    }
}
