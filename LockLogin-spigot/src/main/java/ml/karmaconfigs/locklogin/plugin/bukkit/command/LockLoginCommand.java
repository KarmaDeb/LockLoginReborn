package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.locklogin.api.LockLoginListener;
import ml.karmaconfigs.locklogin.api.event.plugin.UpdateRequestEvent;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.util.PluginCommandType;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.FileReloader;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.ConsoleAccount;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bukkit.permission.PluginPermission.*;

public final class LockLoginCommand extends PluginCommandType implements CommandExecutor {

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Message messages = new Message();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            switch (args.length) {
                case 0:
                    user.send("&5&oAvailable sub-commands:&7 /locklogin &e<reload>");
                    break;
                case 1:
                    switch (args[0].toLowerCase()) {
                        case "reload":
                            if (player.hasPermission(reload())) {
                                FileReloader.reload(player);
                            } else {
                                user.send(messages.prefix() + messages.permissionError(reload()));
                            }
                            break;
                        case "applyupdates":
                            if (player.hasPermission(applyUpdates())) {
                                //BukkitManager.update(sender);
                                UpdateRequestEvent event = new UpdateRequestEvent(sender, player.hasPermission(applyUnsafeUpdates()), null);
                                LockLoginListener.callEvent(event);
                            } else {
                                user.send(messages.prefix() + messages.permissionError(applyUpdates()));
                            }
                            break;
                        default:

                    }
            }
        } else {
            ConsoleAccount console = new ConsoleAccount();

            if (console.isRegistered()) {
                switch (args.length) {
                    case 0:
                        Console.send(messages.prefix() + "&5&oAvailable sub-commands: /locklogin &e<reload> <password>");
                        break;
                    case 2:
                        switch (args[0].toLowerCase()) {
                            case "reload":
                                if (console.validate(args[1])) {
                                    FileReloader.reload(null);
                                } else {
                                    Console.send(messages.prefix() + messages.incorrectPassword());
                                }
                                break;
                            case "applyupdates":
                                if (console.validate(args[1])) {
                                    //BukkitManager.update(sender);
                                    //BukkitManager.unload();
                                    UpdateRequestEvent event = new UpdateRequestEvent(sender, sender.hasPermission(applyUnsafeUpdates()), null);
                                    LockLoginListener.callEvent(event);
                                } else {
                                    Console.send(messages.prefix() + messages.incorrectPassword());
                                }
                                break;
                            default:

                        }
                }
            } else {
                Console.send(messages.prefix() + properties.getProperty("console_not_registered", "&5&oThe console must register to run protected commands!"));
            }
        }
        return false;
    }

    /**
     * Get the plugin command name
     *
     * @return the plugin command
     */
    @Override
    public String command() {
        return "locklogin";
    }
}
