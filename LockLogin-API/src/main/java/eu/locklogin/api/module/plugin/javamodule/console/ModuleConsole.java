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

import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.utils.StringUtils;

/**
 * LockLogin module console manager
 */
public final class ModuleConsole {

    private final PluginModule module;

    /**
     * Initialize the java module console manager
     *
     * @param owner the owner module
     */
    public ModuleConsole(final PluginModule owner) {
        module = owner;
    }

    /**
     * Send a message to the console
     *
     * @param message the message
     * @param replaces the message replaces
     */
    public final void sendMessage(final String message, final Object... replaces) {
        if (ModuleLoader.isLoaded(module)) {
            APISource.getConsole().send(getPrefixManager().getPrefix() +  message, replaces);
        }
    }

    /**
     * Send a message to the console
     *
     * @param level the message level
     * @param message the message
     * @param replaces the message replaces
     */
    public final void sendMessage(final MessageLevel level, final String message, final Object... replaces) {
        if (ModuleLoader.isLoaded(module)) {
            String prefix = getPrefixManager().forLevel(level);

            String final_message = StringUtils.formatString(message, replaces);
            if (!prefix.endsWith(" "))
                prefix = prefix + " ";

            APISource.getConsole().send("{0}{1}", prefix, final_message);
        }
    }

    /**
     * Get the module prefix manager
     *
     * @return the module console prefix manager
     */
    public final ConsolePrefix getPrefixManager() {
        return new ConsolePrefix(module);
    }
}
