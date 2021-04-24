package ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.options;

import ml.karmaconfigs.locklogin.plugin.common.utils.enums.CaptchaType;

public final class CaptchaConfig {

    private final CaptchaType type;
    private final int timeout;
    private final int length;
    private final boolean letters;
    private final boolean strikethrough;
    private final boolean randomStrikethrough;

    /**
     * Initialize the captcha configuration
     *
     * @param mode the captcha mode
     * @param complexTimeOut the complex mode time out
     * @param captchaLength the captcha code length
     * @param includeLetters include letters in code
     * @param enableStrike enable strikethrough effect on captcha
     * @param randomStrike randomize characters with strike effect
     */
    public CaptchaConfig(final String mode, final int complexTimeOut, final int captchaLength, final boolean includeLetters, final boolean enableStrike, final boolean randomStrike) {
        type = CaptchaType.valueOf(mode);
        timeout = complexTimeOut;
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
    public CaptchaType getMode() {
        return type;
    }

    /**
     * Get the captcha time out
     *
     * @return the captcha complex mode
     * timeout
     */
    public int getTimeout() {
        return timeout;
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
}
