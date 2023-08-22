package eu.locklogin.api.file.options;

import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.security.Password;
import eu.locklogin.api.security.PasswordAttribute;
import eu.locklogin.api.util.enums.CheckType;
import eu.locklogin.api.util.platform.CurrentPlatform;
import lombok.Getter;
import lombok.experimental.Accessors;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Password configuration
 */
public final class PasswordConfig {

    @Getter
    @Accessors(fluent = true)
    private final boolean printSuccess;
    @Getter
    @Accessors(fluent = true)
    private final boolean block_unsafe;
    @Getter
    @Accessors(fluent = true)
    private final boolean warn_unsafe;
    @Getter
    @Accessors(fluent = true)
    private final boolean ignore_common;

    @Getter
    @Accessors(fluent = true)
    private final int min_length;
    @Getter
    @Accessors(fluent = true)
    private final int min_characters;
    @Getter
    @Accessors(fluent = true)
    private final int min_numbers;
    @Getter
    @Accessors(fluent = true)
    private final int min_upper;
    @Getter
    @Accessors(fluent = true)
    private final int min_lower;

    /**
     * Initialize the password configuration
     *
     * @param print_success print success messages
     * @param block_unsafe block unsafe passwords
     * @param warn_unsafe warn about unsafe passwords
     * @param ignore_cm ignore common passwords
     * @param min_length password required length
     * @param min_characters password min amount of special characters
     * @param min_numbers password min amount of numbers
     * @param min_upper password min amount of uppercase letters
     * @param min_lower password min amount of lowercase letters
     */
    public PasswordConfig(final boolean print_success, final boolean block_unsafe, final boolean warn_unsafe, final boolean ignore_cm, final int min_length, final int min_characters, final int min_numbers, final int min_upper, final int min_lower) {
        this.printSuccess = print_success;
        this.block_unsafe = block_unsafe;
        this.warn_unsafe = warn_unsafe;
        this.ignore_common = ignore_cm;

        this.min_length = Math.max(min_length, min_characters + min_numbers + min_upper + min_lower);
        this.min_characters = min_characters;
        this.min_numbers = min_numbers;
        this.min_upper = min_upper;
        this.min_lower = min_lower;
    }

    /**
     * Check the password
     *
     * @param password the password to validate
     * @return the failed checks
     */
    public Map.Entry<Boolean, String[]> check(final String password) {
        List<String> result = new ArrayList<>();

        int fails = 0;
        int chars = 0;
        int numbers = 0;
        int uppers = 0;
        int lowers = 0;
        for (char ch : password.toCharArray()) {
            if (Character.isDigit(ch)) {
                numbers++;
                continue;
            }
            if (Character.isUpperCase(ch)) {
                uppers++;
                continue;
            }
            if (Character.isLowerCase(ch)) {
                lowers++;
                continue;
            }
            String str = String.valueOf(ch);
            if (str.matches("[^A-Za-z0-9 ]")) {
                chars++;
            }
        }

        int length = password.length();
        PasswordAttribute attribute = new PasswordAttribute(length, chars, numbers, uppers, lowers);
        PluginMessages messages = CurrentPlatform.getMessages();

        if (!ignore_common) {
            Password pwd = new Password(password);
            if (pwd.isSecure()) {
                if (printSuccess) result.add(messages.checkSuccess(CheckType.UNIQUE, attribute));
            } else {
                fails++;
                result.add(messages.checkFailed(CheckType.UNIQUE, attribute));
            }
        }

        if (length < min_length) {
            fails++;
            result.add(messages.checkFailed(CheckType.LENGTH, attribute));
        } else {
            if (printSuccess) result.add(messages.checkSuccess(CheckType.LENGTH, attribute));
        }

        if (chars < min_characters) {
            fails++;
            result.add(messages.checkFailed(CheckType.SPECIAL, attribute));
        } else {
            if (printSuccess) result.add(messages.checkSuccess(CheckType.SPECIAL, attribute));
        }

        if (numbers < min_numbers) {
            fails++;
            result.add(messages.checkFailed(CheckType.NUMBER, attribute));
        } else {
            if (printSuccess) result.add(messages.checkSuccess(CheckType.NUMBER, attribute));
        }

        if (uppers < min_upper) {
            fails++;
            result.add(messages.checkFailed(CheckType.UPPER, attribute));
        } else {
            if (printSuccess) result.add(messages.checkSuccess(CheckType.UPPER, attribute));
        }

        if (lowers < min_lower) {
            fails++;
            result.add(messages.checkFailed(CheckType.LOWER, attribute));
        } else {
            if (printSuccess) result.add(messages.checkSuccess(CheckType.LOWER, attribute));
        }

        result.add("");
        result.add(StringUtils.formatString("&7Fix &e{0}&7 problems to make your password safe", fails));

        return new AbstractMap.SimpleEntry<>(fails == 0, result.toArray(new String[0]));
    }
}
