package ml.karmaconfigs.locklogin.plugin.common.security.client;

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

import ml.karmaconfigs.api.common.utils.StringUtils;

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
     * Check the client name
     */
    public final void check() {
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);

            if (!Character.isLetterOrDigit(character) && character != '_')
                invalid_chars.add(character);
        }

        checked = true;
    }

    /**
     * Check if the name is valid or not
     *
     * @return if the name is valid
     */
    public final boolean notValid() {
        if (!checked)
            check();

        return !invalid_chars.isEmpty();
    }

    /**
     * Get the name invalid characters
     *
     * @return the name invalid characters
     */
    public final String getInvalidChars() {
        StringBuilder builder = new StringBuilder();
        for (Character character : invalid_chars) {
            String value = String.valueOf(character);

            if (value.replaceAll("\\s", "").isEmpty()) {
                value = "spaces";
            }
            if (value.replaceAll("\\s", "").equals(",")) {
                value = "commas";
            }

            builder.append("&c").append(value).append("&7, ");
        }

        return StringUtils.replaceLast(builder.toString(), "&7, ", "");
    }
}
