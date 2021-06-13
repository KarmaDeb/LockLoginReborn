package eu.locklogin.api.module.plugin.javamodule.console;

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
import eu.locklogin.api.module.PluginModule;

import java.util.HashMap;
import java.util.Map;

/**
 * LockLogin module console prefix
 */
public final class ConsolePrefix {

    private final PluginModule module;

    private final static Map<PluginModule, Map<MessageLevel, String>> prefixes = new HashMap<>();

    /**
     * Initialize the console prefix
     *
     * @param owner the module owner
     */
    public ConsolePrefix(final PluginModule owner) {
        module = owner;
    }

    /**
     * Set the module prefix
     *
     * @param level the message level prefix
     * @param prefix the prefix
     * @throws IllegalArgumentException if the module tries
     * to modify the default prefix or the prefix length is over 16 or is empty
     */
    public final void setPrefix(final MessageLevel level, final String prefix) throws IllegalArgumentException {
        if (StringUtils.isNullOrEmpty(StringUtils.stripColor(prefix)) || StringUtils.stripColor(prefix).length() > 16) {
            throw new IllegalArgumentException("Module " + module.name() + " tried to register an empty prefix or longer than 16 characters");
        }

        Map<MessageLevel, String> data = prefixes.getOrDefault(module, new HashMap<>());

        switch (level) {
            case INFO:
            case WARNING:
            case ERROR:
                data.put(level, prefix);
                prefixes.put(module, data);
                break;
            case NONE:
            default:
                throw new IllegalArgumentException("Module " + module.name() + " tried to modify default prefix");
        }
    }

    /**
     * Get the module prefix
     *
     * @return the module prefix
     */
    public final String getPrefix() {
        return "&7[ &e" + module.name() + "&7 ]&r ";
    }

    /**
     * Get the prefix for the specified message level
     *
     * @param level the message level
     * @return the message for the specified message level
     */
    public final String forLevel(final MessageLevel level) {
        if (level.equals(MessageLevel.NONE))
            return getPrefix();

        Map<MessageLevel, String> data = prefixes.getOrDefault(module, new HashMap<>());
        return data.getOrDefault(level, getPrefix());
    }
}
