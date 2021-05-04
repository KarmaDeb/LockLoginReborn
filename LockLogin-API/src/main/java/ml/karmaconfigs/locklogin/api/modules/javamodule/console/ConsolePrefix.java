package ml.karmaconfigs.locklogin.api.modules.javamodule.console;

import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;

import java.util.HashMap;
import java.util.Map;

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
