package eu.locklogin.api.module.plugin.javamodule;

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
import eu.locklogin.api.module.plugin.api.channel.ModuleMessageService;
import eu.locklogin.api.module.plugin.api.command.Command;
import eu.locklogin.api.module.plugin.api.command.CommandData;
import eu.locklogin.api.module.plugin.api.event.ModuleEventHandler;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginProcessCommandEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.api.event.util.EventListener;
import eu.locklogin.api.module.plugin.javamodule.sender.ModuleSender;
import eu.locklogin.api.module.plugin.javamodule.updater.JavaModuleVersion;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.Logger;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

/**
 * LockLogin java module manager
 */
@SuppressWarnings("unused")
public final class ModulePlugin {

    private final static Logger logger = new Logger(APISource.getSource());

    private final static Map<PluginModule, Set<EventListener>> module_listeners = new LinkedHashMap<>();
    private final static Map<PluginModule, Set<Command>> module_commands = new LinkedHashMap<>();

    private final PluginModule module;

    /**
     * Initialize the java module manager
     *
     * @param owner the owner module
     */
    public ModulePlugin(final PluginModule owner) {
        module = owner;
    }

    /**
     * Get the module commands data
     *
     * @return the module commands data
     */
    public static Set<CommandData> getCommandsData() {
        Set<Command> all_commands = new LinkedHashSet<>();
        for (PluginModule module : module_commands.keySet()) {
            Set<Command> current = module_commands.getOrDefault(module, Collections.emptySet());
            if (!current.isEmpty())
                all_commands.addAll(current);
        }

        Set<CommandData> data = new LinkedHashSet<>();

        for (Command registered : all_commands)
            data.add(new CommandData(registered));

        return data;
    }

    /**
     * Call an event to all the listeners
     *
     * @param event the event to call
     */
    public static void callEvent(final Event event) {
        Set<Method> last_invocation = new LinkedHashSet<>();
        Set<Method> normal_invocation = new LinkedHashSet<>();
        Set<PluginModule> passed = new LinkedHashSet<>();

        Map<PluginModule, Method> after = new LinkedHashMap<>();
        Map<PluginModule, PluginModule> afterOwner = new LinkedHashMap<>();

        for (PluginModule module : module_listeners.keySet()) {
            //Allow only loaded modules
            if (ModuleLoader.isLoaded(module)) {
                Set<EventListener> handlers = module_listeners.getOrDefault(module, Collections.emptySet());

                for (EventListener handler : handlers) {
                    Method[] methods = handler.getClass().getMethods();
                    for (Method method : methods) {
                        if (method.isAnnotationPresent(ModuleEventHandler.class)) {
                            if (method.getParameterTypes()[0].isAssignableFrom(event.getClass())) {
                                ModuleEventHandler methodHandler = method.getAnnotation(ModuleEventHandler.class);
                                switch (methodHandler.priority()) {
                                    case FIRST:
                                        if (methodHandler.ignoreHandled() && event.isHandled())
                                            continue;

                                        try {
                                            method.invoke(handler, event);

                                            logger.scheduleLog(Level.INFO, "Passed event {0} into module {1}", event.getClass().getSimpleName(), module.name());
                                        } catch (Throwable ignored) {}
                                        passed.add(module);
                                        break;
                                    case LAST:
                                        if (methodHandler.ignoreHandled() && event.isHandled())
                                            continue;

                                        last_invocation.add(method);
                                        break;
                                    case AFTER:
                                        if (methodHandler.ignoreHandled() && event.isHandled())
                                            continue;

                                        if (!passed.contains(module)) {
                                            File moduleFile = ModuleLoader.getModuleFile(methodHandler.after());
                                            if (moduleFile != null) {
                                                PluginModule afterModule = ModuleLoader.getByFile(moduleFile);
                                                if (afterModule != null) {
                                                    after.put(afterModule, method);
                                                    afterOwner.put(afterModule, module);
                                                } else {
                                                    try {
                                                        method.invoke(handler, event);
                                                        passed.add(module);
                                                    } catch (Throwable ex) {
                                                        ex.printStackTrace();
                                                    }
                                                }
                                            }
                                        } else {
                                            try {
                                                method.invoke(handler, event);

                                                logger.scheduleLog(Level.INFO, "Passed event {0} into module {1}", event.getClass().getSimpleName(), module.name());
                                            } catch (Throwable ignored) {}
                                        }
                                    case NORMAL:
                                    default:
                                        if (methodHandler.ignoreHandled() && event.isHandled())
                                            continue;

                                        normal_invocation.add(method);
                                        break;
                                }
                            }
                        }
                    }

                    for (Method method : normal_invocation) {
                        try {
                            method.invoke(handler, event);
                            passed.add(module);

                            if (after.containsKey(module) && after.getOrDefault(module, null) != null) {
                                try {
                                    after.get(module).invoke(handler, event);
                                    passed.add(afterOwner.get(module));
                                } catch (Throwable ex) {
                                    ex.printStackTrace();
                                }
                            }

                            logger.scheduleLog(Level.INFO, "Passed event {0} into module {1}", event.getClass().getSimpleName(), module.name());
                        } catch (Throwable ignored) {}
                    }

                    for (Method method : last_invocation) {
                        try {
                            method.invoke(handler, event);
                            passed.add(module);

                            if (after.containsKey(module) && after.getOrDefault(module, null) != null) {
                                try {
                                    after.get(module).invoke(handler, event);
                                    passed.add(afterOwner.get(module));
                                } catch (Throwable ex) {
                                    ex.printStackTrace();
                                }
                            }

                            logger.scheduleLog(Level.INFO, "Passed event {0} into module {1}", event.getClass().getSimpleName(), module.name());
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }
    }

    /**
     * Fire a command
     *
     * @param sender the command sender
     * @param cmd    the command
     * @param parentEvent the parent event
     */
    public static void fireCommand(final ModuleSender sender, final String cmd, final Object parentEvent) {
        String argument = cmd.substring(1);

        if (argument.contains(" "))
            argument = argument.split(" ")[0];

        if (isValid(argument)) {
            String[] arguments;
            String parsed_message = cmd.substring(1).replaceFirst(argument + " ", "");
            if (parsed_message.contains(" ")) {
                arguments = parsed_message.split(" ");
            } else {
                if (!parsed_message.equals(argument)) {
                    arguments = new String[]{parsed_message};
                } else {
                    arguments = new String[]{};
                }
            }

            for (PluginModule module : module_commands.keySet()) {
                //Allow only loaded modules
                if (ModuleLoader.isLoaded(module)) {
                    Set<Command> handlers = module_commands.getOrDefault(module, Collections.emptySet());

                    for (Command handler : handlers) {
                        //Only call the event if the event class is instance of the
                        //listener class
                        List<String> valid_args = Arrays.asList(handler.validAliases());
                        if (valid_args.stream().anyMatch(argument::equalsIgnoreCase)) {
                            try {
                                Event event = new PluginProcessCommandEvent(argument, sender, parentEvent, arguments);
                                ModulePlugin.callEvent(event);

                                if (!event.isHandled()) {
                                    handler.processCommand(argument, sender, arguments);

                                    logger.scheduleLog(Level.INFO, "Passed command {0} ( from module {1} ) into sender {2}",
                                            argument, module.name(), sender.getName());
                                }
                            } catch (Throwable ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get all the available commands
     *
     * @return all the available commands
     */
    private static Set<String> availableCommands() {
        Set<String> cmds = new LinkedHashSet<>();

        for (PluginModule module : module_commands.keySet()) {
            Set<Command> handlers = module_commands.getOrDefault(module, new LinkedHashSet<>());

            for (Command handler : handlers)
                cmds.addAll(Arrays.asList(handler.validAliases()));
        }

        return cmds;
    }

    /**
     * Get the module that owns the specified command
     *
     * @param command the command
     * @return the module owning the specified command
     */
    public static PluginModule getCommandOwner(String command) {
        if (command.startsWith(CurrentPlatform.getPrefix()))
            command = command.substring(1);

        for (PluginModule module : module_commands.keySet()) {
            Set<Command> handlers = module_commands.getOrDefault(module, new LinkedHashSet<>());

            for (Command handler : handlers) {
                if (Arrays.stream(handler.validAliases()).anyMatch(command::equalsIgnoreCase))
                    return module;
            }
        }

        return null;
    }

    /**
     * Check if the command is valid
     *
     * @param command the command
     * @return if the command is valid
     */
    private static boolean isValid(String command) {
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
    public static boolean parseCommand(String message) {
        if (message.startsWith("$") && CurrentPlatform.getPrefix().equals("/"))
            message = message.replace("$", "/");

        if (message.startsWith(CurrentPlatform.getPrefix()))
            return isValid((message.contains(" ") ? message.split(" ")[0] : message));

        return false;
    }

    /**
     * Register a new listener to the module
     *
     * @param event the event listener to register
     * @throws IllegalStateException if the module tries to register
     *                               a listener while not loaded
     */
    public void registerListener(final EventListener event) throws IllegalStateException {
        if (ModuleLoader.isLoaded(module)) {
            Set<EventListener> listeners = module_listeners.getOrDefault(module, Collections.newSetFromMap(new LinkedHashMap<>()));

            logger.scheduleLog(Level.INFO, "Registered event listener {0} of module {1}", event.getClass().getName(), module.name());

            listeners.add(event);
            module_listeners.put(module, listeners);
        } else {
            throw new IllegalStateException("Module " + module.name() + " tried to register a listener while not registered!");
        }
    }

    /**
     * Register a new command to the module
     *
     * @param command the command to register
     * @throws IllegalStateException if the module tries to register a command
     *                               while not registered
     */
    public void registerCommand(final Command command) throws IllegalStateException {
        if (ModuleLoader.isLoaded(module)) {
            Set<Command> commands = module_commands.getOrDefault(module, Collections.newSetFromMap(new LinkedHashMap<>()));

            logger.scheduleLog(Level.INFO, "Registered command {0} of module {1}", command.validAliases()[0], module.name());

            commands.add(command);
            module_commands.put(module, commands);
        } else {
            throw new IllegalStateException("Module " + module.name() + " tried to register a listener while not registered!");
        }
    }

    /**
     * Remove a listener from the registered listeners
     *
     * @param event the listener to remove
     */
    public void unregisterListener(final EventListener event) {
        Set<EventListener> listeners = module_listeners.getOrDefault(module, Collections.newSetFromMap(new LinkedHashMap<>()));
        for (EventListener listener : listeners) {
            if (listener.getClass().getName().equals(event.getClass().getName())) {
                logger.scheduleLog(Level.INFO, "Unregistered event listener {0} of module {1}", event.getClass().getName(), module.name());
                listeners.remove(event);
            }
        }

        module_listeners.put(module, listeners);
    }

    /**
     * Unregister a command from the registered commands
     *
     * @param command the command to remove
     */
    public void unregisterCommand(final Command command) {
        Set<Command> commands = module_commands.getOrDefault(module, Collections.newSetFromMap(new LinkedHashMap<>()));
        for (Command cmd : commands) {
            if (cmd.getClass().getName().equals(command.getClass().getName())) {
                logger.scheduleLog(Level.INFO, "Unregistered command {0} of module {1}", command.validAliases()[0], module.name());
                commands.remove(command);
            }
        }

        module_commands.put(module, commands);
    }

    /**
     * Unregister all the listeners
     */
    public void unregisterListeners() {
        module_listeners.clear();
    }

    /**
     * Unregister all the commands
     */
    public void unregisterCommands() {
        module_commands.remove(module);
    }

    /**
     * Get all the registered listeners
     *
     * @return all the registered listeners
     */
    public Set<EventListener> getRegisteredListeners() {
        return module_listeners.getOrDefault(module, Collections.newSetFromMap(new LinkedHashMap<>()));
    }

    /**
     * Get all the registered commands
     *
     * @return all the registered commands
     */
    public Set<Command> getRegisteredCommands() {
        return module_commands.getOrDefault(module, Collections.emptySet());
    }

    /**
     * Get the module version manager
     *
     * @return the module version manager
     */
    public JavaModuleVersion getVersionManager() {
        return new JavaModuleVersion(module);
    }

    /**
     * Get the module messenger
     *
     * @return the module messenger
     */
    public ModuleMessageService getMessenger() {
        return new ModuleMessageService(module);
    }
}
