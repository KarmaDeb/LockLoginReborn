package eu.locklogin.plugin.bungee.plugin.injector;

import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.command.ConsoleCommandSender;

import static eu.locklogin.plugin.bungee.LockLogin.logger;
import static eu.locklogin.plugin.bungee.LockLogin.plugin;

public final class BungeeInjector extends Injector {

    /**
     * Inject into BungeeCord terminal command
     * parser
     * <p>
     * Basically I make BungeeCord end his command
     * parsing and then placing mine which respects
     * how BungeeCord parsed his command originally
     */
    @Override
    public void inject() {
        try {
            BungeeCord bungee = BungeeCord.getInstance();
            if (bungee.isRunning) {
                //Sorry md_5, I need this to
                //bypass your command limitations
                bungee.isRunning = false;
            }

            plugin.async().queue(() -> {
                String line;
                try {
                    while ((line = bungee.getConsoleReader().readLine(">")) != null) {
                        if (line.startsWith("end")) {
                            bungee.isRunning = true;
                        }

                        if (!bungee.getPluginManager().dispatchCommand(ConsoleCommandSender.getInstance(), line)) {
                            boolean fallback = true;

                            if (ModulePlugin.parseCommand(line)) {
                                PluginModule module = ModulePlugin.getCommandOwner(line);

                                if (module != null) {
                                    ModulePlugin.fireCommand(module.getConsole(), line, null);
                                    fallback = false;
                                }
                            }

                            if (fallback) {
                                bungee.getConsole().sendMessage(new ComponentBuilder("Command not found").color(ChatColor.RED).create());
                            }
                        }
                    }
                } catch (Throwable ex) {
                    logger.scheduleLog(Level.GRAVE, ex);
                    logger.scheduleLog(Level.INFO, "LockLogin unhooked md_5's BungeeCord command handler due an internal error");

                    //We don't want people to stop
                    //being able to type commands at
                    //the first error
                    inject();
                }
            });

            plugin.console().send("Injected LockLogin's command parser for terminal to allow module commands", Level.WARNING);
        } catch (Throwable ex) {
            ex.printStackTrace();
            plugin.console().send("Failed to inject into BungeeCord's command sender, this will result in modules not able to run console commands", Level.GRAVE);
        }
    }
}
