package eu.locklogin.api.module.plugin.javamodule.sender;

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
import eu.locklogin.api.module.plugin.api.event.server.ModuleServerMessageEvent;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.console.ConsolePrefix;
import eu.locklogin.api.module.plugin.javamodule.console.MessageLevel;
import ml.karmaconfigs.api.common.karma.APISource;

/**
 * LockLogin module console manager
 */
public final class ModuleConsole extends ModuleSender {

    private final PluginModule module;

    /**
     * Initialize the java module console manager
     *
     * @param owner the owner module
     */
    public ModuleConsole(final PluginModule owner) {
        super();
        module = owner;
    }

    /**
     * Send a message to the console
     *
     * @param message the message
     * @param replaces the message replaces
     */
    public void sendMessage(final String message, final Object... replaces) {
        if (ModuleLoader.isLoaded(module)) {
            ModuleServerMessageEvent event = new ModuleServerMessageEvent(module, message, replaces);
            ModulePlugin.callEvent(event);

            if (!event.isHandled()) {
                APISource.getConsole().send(getPrefixManager().getPrefix() + event.getMessage(), event.getReplaces());
            }
        }
    }

    /**
     * Send a message to the console
     *
     * @param level the message level
     * @param message the message
     * @param replaces the message replaces
     */
    public void sendMessage(final MessageLevel level, final String message, final Object... replaces) {
        if (ModuleLoader.isLoaded(module)) {
            String prefix = getPrefixManager().forLevel(level);

            ModuleServerMessageEvent event = new ModuleServerMessageEvent(module, message, replaces);
            ModulePlugin.callEvent(event);

            APISource.getConsole().send("{0}{1}", prefix, event.getMessage(), event.getReplaces());
        }
    }

    /**
     * Get the module prefix manager
     *
     * @return the module console prefix manager
     */
    public ConsolePrefix getPrefixManager() {
        return new ConsolePrefix(module);
    }
}
