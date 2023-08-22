package eu.locklogin.plugin.bungee.plugin.injector;

import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.api.command.CommandData;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bungee.LockLogin.logger;
import static eu.locklogin.plugin.bungee.LockLogin.plugin;

public class ModuleExecutorInjector extends Injector {

    private final static Map<String, Command> registered = new ConcurrentHashMap<>();

    /**
     * Waterfall uses a different method
     * of parsing commands, so the only solution
     * is to register all the module commands
     * followed of {@link PluginConfiguration#getModulePrefix()} pre-argument.
     * <p>
     * The bad thing about this is that players
     * will be able to perform /{@link PluginConfiguration#getModulePrefix()} command
     */
    @Override
    public void inject() {
        String alias = CurrentPlatform.getConfiguration().getModulePrefix();

        Set<Command> remove = Collections.newSetFromMap(new ConcurrentHashMap<>());
        for (Map.Entry<String, Command> cmd : plugin.getProxy().getPluginManager().getCommands()) {
            try {
                String command = cmd.getKey().substring(1);
                if (registered.containsKey(command)) {
                    if (!cmd.getKey().startsWith(alias)) {
                        remove.add(cmd.getValue());
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        for (Command del : remove)
            plugin.getProxy().getPluginManager().unregisterCommand(del);

        for (CommandData data : ModulePlugin.getCommandsData()) {
            String[] aliases = data.getOwner().validAliases();
            String cmd = alias + aliases[0];
            String[] fixedAliases = new String[0];
            try {
                fixedAliases = new String[aliases.length - 1];

                for (int i = 1; i < aliases.length; i++) {
                    fixedAliases[(i - 1)] = alias + aliases[i];
                }
            } catch (Throwable ignored) {
            }

            Command command = new Command(cmd, "", fixedAliases) {
                @Override
                public void execute(final CommandSender commandSender, final String[] strings) {
                    if (!(commandSender instanceof ProxiedPlayer)) {
                        StringBuilder builder = new StringBuilder(cmd);
                        for (String arg : strings) builder.append(" ").append(arg);

                        String message = builder.toString();
                        logger.scheduleLog(Level.INFO, "Injected command {0}", message);

                        if (ModulePlugin.parseCommand(message)) {
                            PluginModule module = ModulePlugin.getCommandOwner(cmd);

                            if (module != null) {
                                ModulePlugin.fireCommand(module.getConsole(), message, null);
                            }
                        }
                    }
                }
            };
            registered.put(cmd.substring(1), command);
            plugin.getProxy().getPluginManager().registerCommand(plugin, command);
        }

        plugin.console().send("Injected LockLogin's commands for BungeeCord/Waterfall", Level.WARNING);
    }
}
