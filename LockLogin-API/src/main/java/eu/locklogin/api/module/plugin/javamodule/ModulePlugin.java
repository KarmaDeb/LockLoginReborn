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
import eu.locklogin.api.module.plugin.api.event.user.GenericJoinEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.api.event.util.EventListener;
import eu.locklogin.api.module.plugin.javamodule.sender.ModuleConsole;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.sender.ModuleSender;
import eu.locklogin.api.module.plugin.javamodule.updater.JavaModuleVersion;
import eu.locklogin.api.util.platform.CurrentPlatform;

import java.lang.reflect.Method;
import java.util.*;

/**
 * LockLogin java module manager
 */
public final class ModulePlugin {

    private final static Map<PluginModule, Set<eu.locklogin.api.module.plugin.api.event.util.EventListener>> module_listeners = new LinkedHashMap<>();
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
                    //Only call the event if the event class is instance of the
                    //listener class
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
                                        } catch (Throwable ex) {
                                            ex.printStackTrace();
                                        }
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
                                            PluginModule afterModule = ModuleLoader.getByFile(ModuleLoader.getModuleFile(methodHandler.after()));
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
                                        } else {
                                            try {
                                                method.invoke(handler, event);
                                            } catch (Throwable ex) {
                                                ex.printStackTrace();
                                            }
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
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
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
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * Call an event to all the listeners
     *
     * @param event the event to call
     */
    public static void callEvent(final GenericJoinEvent event) {
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
                    //Only call the event if the event class is instance of the
                    //listener class
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
                                        } catch (Throwable ex) {
                                            ex.printStackTrace();
                                        }
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
                                            PluginModule afterModule = ModuleLoader.getByFile(ModuleLoader.getModuleFile(methodHandler.after()));
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
                                        } else {
                                            try {
                                                method.invoke(handler, event);
                                            } catch (Throwable ex) {
                                                ex.printStackTrace();
                                            }
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
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
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
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
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
                                PluginProcessCommandEvent event = new PluginProcessCommandEvent(argument, sender, parentEvent, arguments);
                                ModulePlugin.callEvent(event);

                                if (!event.isHandled()) {
                                    Method processCommandMethod = handler.getClass().getMethod("processCommand", String.class, ModuleSender.class, String[].class);
                                    processCommandMethod.invoke(handler, argument, sender, arguments);
                                }
                            } catch (Throwable ex) {
                                try {
                                    //Legacy API support
                                    PluginProcessCommandEvent event = new PluginProcessCommandEvent(argument, sender, parentEvent, arguments);
                                    ModulePlugin.callEvent(event);

                                    if (!event.isHandled()) {
                                        Method processCommandMethod = handler.getClass().getMethod("processCommand", String.class, Object.class, String[].class);
                                        processCommandMethod.invoke(handler, argument, (sender instanceof ModulePlayer ? ((ModulePlayer) sender).getPlayer() : sender), arguments);
                                    }
                                } catch (Throwable exc) {
                                    exc.printStackTrace();
                                }
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
    public final void registerListener(final eu.locklogin.api.module.plugin.api.event.util.EventListener event) throws IllegalStateException {
        if (ModuleLoader.isLoaded(module)) {
            Set<eu.locklogin.api.module.plugin.api.event.util.EventListener> current = getRegisteredListeners();
            Set<eu.locklogin.api.module.plugin.api.event.util.EventListener> updated = new LinkedHashSet<>();

            boolean added = false;
            for (eu.locklogin.api.module.plugin.api.event.util.EventListener registered : current) {
                if (registered.getClass().getName().equals(event.getClass().getName())) {
                    added = true;
                    updated.add(event);
                } else {
                    updated.add(registered);
                }
            }

            if (!added)
                updated.add(event);

            module_listeners.put(module, updated);
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
    public final void registerCommand(final Command command) throws IllegalStateException {
        if (ModuleLoader.isLoaded(module)) {
            Set<Command> current = getRegisteredCommands();
            Set<Command> updated = new LinkedHashSet<>();

            boolean added = false;
            for (Command registered : current) {
                if (registered.getClass().getName().equals(command.getClass().getName())) {
                    added = true;
                    updated.add(command);
                } else {
                    updated.add(registered);
                }
            }

            if (!added)
                updated.add(command);

            module_commands.put(module, updated);
        } else {
            throw new IllegalStateException("Module " + module.name() + " tried to register a command while not registered!");
        }
    }

    /**
     * Remove a listener from the registered listeners
     *
     * @param listener the listener to remove
     */
    public final void unregisterListener(final eu.locklogin.api.module.plugin.api.event.util.EventListener listener) {
        Set<eu.locklogin.api.module.plugin.api.event.util.EventListener> current = getRegisteredListeners();
        Set<eu.locklogin.api.module.plugin.api.event.util.EventListener> updated = new LinkedHashSet<>();

        for (eu.locklogin.api.module.plugin.api.event.util.EventListener registered : current) {
            if (!registered.getClass().getName().equals(listener.getClass().getName()))
                updated.add(registered);
        }

        module_listeners.put(module, updated);
    }

    /**
     * Unregister a command from the registered commands
     *
     * @param command the command to remove
     */
    public final void unregisterCommand(final Command command) {
        Set<Command> current = getRegisteredCommands();
        Set<Command> updated = new LinkedHashSet<>();

        for (Command registered : current) {
            if (!registered.getClass().getName().equals(command.getClass().getName()))
                updated.add(registered);
        }

        module_commands.put(module, updated);
    }

    /**
     * Unregister all the listeners
     */
    public final void unregisterListeners() {
        module_listeners.remove(module);
    }

    /**
     * Unregister all the commands
     */
    public final void unregisterCommands() {
        module_commands.remove(module);
    }

    /**
     * Get all the registered listeners
     *
     * @return all the registered listeners
     */
    public final Set<EventListener> getRegisteredListeners() {
        return module_listeners.getOrDefault(module, Collections.emptySet());
    }

    /**
     * Get all the registered commands
     *
     * @return all the registered commands
     */
    public final Set<Command> getRegisteredCommands() {
        return module_commands.getOrDefault(module, Collections.emptySet());
    }

    /**
     * Get the module console sender
     *
     * @return the module console sender
     * @deprecated the developer can use {@link PluginModule#getConsole()} now
     */
    @Deprecated
    public final ModuleConsole getConsoleSender() {
        return new ModuleConsole(module);
    }

    /**
     * Get the module version manager
     *
     * @return the module version manager
     */
    public final JavaModuleVersion getVersionManager() {
        return new JavaModuleVersion(module);
    }

    /**
     * Get the module messenger
     *
     * @return the module messenger
     */
    public final ModuleMessageService getMessenger() {
        return new ModuleMessageService(module);
    }
}
