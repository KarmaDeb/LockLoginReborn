package eu.locklogin.api.common.security.client;

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

import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.string.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * LockLogin name checker
 */
public final class Name {

    private final String name;

    private final Set<Character> invalid_chars = new LinkedHashSet<>();
    private boolean checked = false;

    /**
     * Initialize the name
     *
     * @param clientName the client name
     */
    public Name(final String clientName) {
        name = clientName;
    }

    /**
     * Get if the character is ascii character
     *
     * @param c the character
     * @return if the character is ascii
     */
    public static boolean notAscii(final Character c) {
        int charId = (int) c;

        if (charId > 127) {
            return true;
        } else {
            if (charId <= 47) {
                return true;
            }
            switch (charId) {
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 91:
                case 92:
                case 93:
                case 94:
                case 96:
                case 123:
                case 124:
                case 125:
                case 126:
                case 127:
                    return true;
                default:
                    return false;
            }
        }
    }

    /**
     * Check the client name
     */
    public void check() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);

            if (config.nameCheckProtocol() == 2) {
                if (notAscii(character)) {
                    invalid_chars.add(character);
                }
            } else {
                if (!Character.isLetterOrDigit(character) && character != '_') {
                    invalid_chars.add(character);
                }
            }
        }

        checked = true;
    }

    /**
     * Check if the name is valid or not
     *
     * @return if the name is valid
     */
    public boolean notValid() {
        if (!checked)
            check();

        return !invalid_chars.isEmpty();
    }

    /**
     * Get the name invalid characters
     *
     * @return the name invalid characters
     */
    public String getInvalidChars() {
        StringBuilder builder = new StringBuilder();
        for (Character character : invalid_chars) {
            String value = String.valueOf(character);

            if (value.replaceAll("\\s", "").isEmpty()) {
                value = "spaces";
            }
            if (value.replaceAll("\\s", "").equals(",")) {
                value = "commas";
            }

            builder.append("&eu.c").append(value).append("&7, ");
        }

        return StringUtils.replaceLast(builder.toString(), "&7, ", "");
    }
}
