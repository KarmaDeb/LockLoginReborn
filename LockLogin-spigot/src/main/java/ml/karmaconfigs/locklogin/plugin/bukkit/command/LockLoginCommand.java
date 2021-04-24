package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.BukkitManager;
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

public final class LockLoginCommand implements CommandExecutor {

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
                    user.send("&cAvailable sub-commands:&7 /locklogin &e<reload>");
                    break;
                case 1:
                    switch (args[0].toLowerCase()) {
                        case "reload":
                            if (player.hasPermission(reload())) {
                                BukkitManager.reload(player);
                            } else {
                                user.send(messages.prefix() + messages.permissionError(reload()));
                            }
                            break;
                        case "applyUpdates":
                            if (player.hasPermission(applyUpdates())) {
                                BukkitManager.update(sender);
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
                                    BukkitManager.reload(null);
                                } else {
                                    Console.send(messages.prefix() + messages.incorrectPassword());
                                }
                                break;
                            case "applyUpdates":
                                if (console.validate(args[1])) {
                                    BukkitManager.update(sender);
                                } else {
                                    Console.send(messages.prefix() + messages.incorrectPassword());
                                }
                                break;
                            default:

                        }
                }
            } else {
                Console.send(messages.prefix() + properties.getProperty("console_not_registered", "&cThe console must register to run protected commands!"));
            }
        }
        return false;
    }
}
