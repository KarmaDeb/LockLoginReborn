package ml.karmaconfigs.locklogin.api.command;

import ml.karmaconfigs.locklogin.api.modules.PluginModule;
import ml.karmaconfigs.locklogin.plugin.common.utils.platform.CurrentPlatform;

import java.lang.reflect.Method;
import java.util.*;

/**
 * LockLogin command
 */
public final class LockLoginCommand {

    private final static Map<PluginModule, Set<Command>> commands = new HashMap<>();

    /**
     * Register a command and link it to
     * the specified module
     *
     * @param owner the module owner
     * @param cmd the command class
     */
    public static void registerCommand(final PluginModule owner, final Command cmd) {
        Set<Command> handlers = commands.getOrDefault(owner, new LinkedHashSet<>());
        handlers.add(cmd);

        commands.put(owner, handlers);
    }

    /**
     * Unregister all the commands of the specified
     * module
     *
     * @param module the module
     */
    public static void unregisterCommands(final PluginModule module) {
        commands.put(module, new HashSet<>());
    }

    /**
     * Fire a command, so each module can handle it
     *
     * @param message the command message
     * @param sender the command sender
     */
    public static void fireCommand(final String message, final Object sender) {
        String argument = message.substring(1);

        if (argument.contains(" "))
            argument = argument.split(" ")[0];

        if (isValid(argument)) {
            String[] arguments;
            String parsed_message = message.substring(1).replaceFirst(argument + " ", "");
            if (parsed_message.contains(" ")) {
                arguments = parsed_message.split(" ");
            } else {
                if (!parsed_message.equals(argument)) {
                    arguments = new String[]{parsed_message};
                } else {
                    arguments = new String[]{};
                }
            }

            for (PluginModule module : commands.keySet()) {
                Set<Command> handlers = commands.getOrDefault(module, new LinkedHashSet<>());

                for (Command handler : handlers) {
                    //Only call the event if the event class is instance of the
                    //listener class
                    List<String> valid_args = Arrays.asList(handler.validArguments());
                    if (valid_args.stream().anyMatch(argument::equalsIgnoreCase)) {
                        try {
                            Method processCommandMethod = handler.getClass().getMethod("processCommand", String.class, Object.class, String[].class);
                            processCommandMethod.invoke(handler, argument, sender, arguments);
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * Get all the modules that have a command on it
     *
     * @return all the modules that have commands
     */
    public static Set<PluginModule> getModules() {
        return commands.keySet();
    }

    /**
     * Get all the available commands
     *
     * @return all the available commands
     */
    public static Set<String> availableCommands() {
        Set<String> cmds = new LinkedHashSet<>();

        for (PluginModule module : getModules()) {
            Set<Command> handlers = commands.getOrDefault(module, new LinkedHashSet<>());

            for (Command handler : handlers)
                cmds.addAll(Arrays.asList(handler.validArguments()));
        }

        return cmds;
    }

    /**
     * Get all the command data to enable
     * $help command
     *
     * @return all the command data
     */
    public static Set<CommandData> getData() {
        Set<CommandData> cmds = new LinkedHashSet<>();

        for (PluginModule module : getModules()) {
            Set<Command> handlers = commands.getOrDefault(module, new LinkedHashSet<>());

            for (Command handler : handlers)
                cmds.add(new CommandData(handler));
        }

        return cmds;
    }

    /**
     * Check if the command is valid
     *
     * @param command the command
     * @return if the command is valid
     */
    public static boolean isValid(String command) {
        if (command.startsWith(CurrentPlatform.getPrefix()))
            command = command.substring(1);

        return availableCommands().stream().anyMatch(command::equalsIgnoreCase);
    }

    /**
     * Get if the parser can parse the command
     *
     * @param message the message the player has sent
     * @return if the module parser can parse the command
     */
    public static boolean parse(String message) {
        if (message.startsWith("$") && CurrentPlatform.getPrefix().equals("/"))
            message = message.replace("$", "/");

        if (message.startsWith(CurrentPlatform.getPrefix()))
            return isValid((message.contains(" ") ? message.split(" ")[0] : message));

        return false;
    }
}

