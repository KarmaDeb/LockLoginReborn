package ml.karmaconfigs.locklogin.api.files.options;

public final class CaptchaConfig {

    private final boolean enabled;
    private final int length;
    private final boolean letters;
    private final boolean strikethrough;
    private final boolean randomStrikethrough;

    /**
     * Initialize the captcha configuration
     *
     * @param toggle         the captcha mode
     * @param captchaLength  the captcha code length
     * @param includeLetters include letters in code
     * @param enableStrike   enable strikethrough effect on captcha
     * @param randomStrike   randomize characters with strike effect
     */
    public CaptchaConfig(final boolean toggle, final int captchaLength, final boolean includeLetters, final boolean enableStrike, final boolean randomStrike) {
        enabled = toggle;
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
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the captcha code length
     *
     * @return the captcha code length
     */
    public final int getLength() {
        return length;
    }

    /**
     * Get if the captcha has letters
     *
     * @return if the captcha has letters
     */
    public final boolean hasLetters() {
        return letters;
    }

    /**
     * Get if the captcha has strike effect
     * enabled
     *
     * @return if the captcha has strike
     */
    public final boolean enableStrike() {
        return strikethrough;
    }

    /**
     * Get if the captcha should randomize
     * the characters that get strike
     * effect
     *
     * @return if the strike effect is random
     */
    public final boolean randomStrike() {
        return randomStrikethrough;
    }
}
