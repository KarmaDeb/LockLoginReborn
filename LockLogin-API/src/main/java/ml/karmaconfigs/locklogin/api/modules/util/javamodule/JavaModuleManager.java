package ml.karmaconfigs.locklogin.api.modules.util.javamodule;

import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.locklogin.api.modules.api.channel.ModuleMessageService;
import ml.karmaconfigs.locklogin.api.modules.util.ModuleDependencyLoader;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;
import ml.karmaconfigs.locklogin.api.modules.api.command.Command;
import ml.karmaconfigs.locklogin.api.modules.api.command.CommandData;
import ml.karmaconfigs.locklogin.api.modules.util.dependencies.Dependency;
import ml.karmaconfigs.locklogin.api.modules.api.event.ModuleEventHandler;
import ml.karmaconfigs.locklogin.api.modules.api.event.util.Event;
import ml.karmaconfigs.locklogin.api.modules.api.event.util.EventListener;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.updater.JavaModuleVersion;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;

import java.lang.reflect.Method;
import java.util.*;

public final class JavaModuleManager {

    private final static Map<PluginModule, Set<EventListener>> module_listeners = new LinkedHashMap<>();
    private final static Map<PluginModule, Set<Command>> module_commands = new LinkedHashMap<>();
    private final PluginModule module;

    /**
     * Initialize the java module manager
     *
     * @param owner the owner module
     */
    public JavaModuleManager(final PluginModule owner) {
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
            if (JavaModuleLoader.isLoaded(module)) {
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
                                        try {
                                            method.invoke(handler, event);
                                        } catch (Throwable ex) {
                                            ex.printStackTrace();
                                        }
                                        passed.add(module);
                                        break;
                                    case LAST:
                                        last_invocation.add(method);
                                        break;
                                    case AFTER:
                                        if (!passed.contains(module)) {
                                            PluginModule afterModule = JavaModuleLoader.getByName(methodHandler.after());
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
     */
    public static void fireCommand(final Object sender, final String cmd) {
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
                if (JavaModuleLoader.isLoaded(module)) {
                    Set<Command> handlers = module_commands.getOrDefault(module, Collections.emptySet());

                    for (Command handler : handlers) {
                        //Only call the event if the event class is instance of the
                        //listener class
                        List<String> valid_args = Arrays.asList(handler.validAliases());
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
     * Request LockLogin to download and load specified
     * dependencies
     *
     * @param dependencies the dependencies to download
     */
    public final void requestDependencies(final Dependency... dependencies) {
        if (JavaModuleLoader.isLoaded(module)) {
            for (Dependency dependency : dependencies) {
                dependency.setOwner(module);
                ModuleDependencyLoader loader = new ModuleDependencyLoader(dependency.getLocation());
                Throwable error = loader.download(dependency.getDownloadURL());

                if (error != null) {
                    Console.send("&cFailed to download module dependency: " + dependency.getName());
                    error.printStackTrace();
                } else {
                    dependency.inject();
                    loader.inject(CurrentPlatform.getMain());
                    Console.send("&aLoaded and injected module dependency: " + dependency.getName());
                }
            }
        } else {
            throw new IllegalStateException("Module " + module.name() + " tried to request dependencies while not registered!");
        }
    }

    /**
     * Register a new listener to the module
     *
     * @param event the event listener to register
     * @throws IllegalStateException if the module tries to register
     *                               a listener while not loaded
     */
    public final void registerListener(final EventListener event) throws IllegalStateException {
        if (JavaModuleLoader.isLoaded(module)) {
            Set<EventListener> current = getRegisteredListeners();
            Set<EventListener> updated = new LinkedHashSet<>();

            boolean added = false;
            for (EventListener registered : current) {
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
        if (JavaModuleLoader.isLoaded(module)) {
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
    public final void unregisterListener(final EventListener listener) {
        Set<EventListener> current = getRegisteredListeners();
        Set<EventListener> updated = new LinkedHashSet<>();

        for (EventListener registered : current) {
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
     */
    public final JavaConsoleManager getConsoleSender() {
        return new JavaConsoleManager(module);
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
