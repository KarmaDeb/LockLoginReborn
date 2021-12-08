package eu.locklogin.plugin.velocity.plugin.injector;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.api.command.CommandData;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.velocity.LockLogin.server;
import static eu.locklogin.plugin.velocity.LockLogin.source;

public class VelocityInjector extends Injector {

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

        Set<String> remove = Collections.newSetFromMap(new ConcurrentHashMap<>());

        for (CommandData cmd : ModulePlugin.getCommandsData()) {
            try {
                String command = cmd.getOwner().validAliases()[0].substring(1);
                if (registered.containsKey(command)) {
                    if (!cmd.getOwner().validAliases()[0].startsWith(alias)) {
                        remove.add(cmd.getOwner().validAliases()[0]);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        for (String del : remove) {
            server.getCommandManager().unregister(del);
        }

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

            Command command = (SimpleCommand) invocation -> {
                CommandSource commandSender = invocation.source();

                if (commandSender instanceof ConsoleCommandSource) {
                    if (ModulePlugin.parseCommand(cmd)) {
                        PluginModule module = ModulePlugin.getCommandOwner(cmd);

                        if (module != null) {
                            ModulePlugin.fireCommand(module.getConsole(), cmd, null);
                        }
                    }
                }
            };
            registered.put(cmd.substring(1), command);
            server.getCommandManager().register(cmd, command, fixedAliases);
        }

        source.console().send("Injected LockLogin's commands for Velocity", Level.WARNING);
    }
}
