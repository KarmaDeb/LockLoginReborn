package ml.karmaconfigs.locklogin.api.modules.util.javamodule;

import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.console.ConsolePrefix;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.console.MessageLevel;

public final class JavaConsoleManager {

    private final PluginModule module;

    /**
     * Initialize the java module console manager
     *
     * @param owner the owner module
     */
    public JavaConsoleManager(final PluginModule owner) {
        module = owner;
    }

    /**
     * Send a message to the console
     *
     * @param message the message
     * @param replaces the message replaces
     */
    public final void sendMessage(final String message, final Object... replaces) {
        if (JavaModuleLoader.isLoaded(module)) {
            Console.send(message, replaces);
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
        if (JavaModuleLoader.isLoaded(module)) {
            String prefix = getPrefixManager().forLevel(level);

            String final_message = StringUtils.formatString(message, replaces);
            if (!prefix.endsWith(" "))
                prefix = prefix + " ";

            Console.send("{0}{1}", prefix, final_message);
        }
    }

    /**
     * Get the module prefix manager
     */
    public final ConsolePrefix getPrefixManager() {
        return new ConsolePrefix(module);
    }
}
