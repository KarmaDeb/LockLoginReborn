package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.util.PluginCommandType;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.ConsoleAccount;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.lock.LockedAccount;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.lock.LockedData;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bukkit.permission.PluginPermission.*;

public class UnLockCommand extends PluginCommandType implements CommandExecutor {

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

            if (user.getSession().isValid()) {
                if (player.hasPermission(account())) {
                    if (args.length == 1) {
                        String target = args[0];
                        OfflineClient offline = new OfflineClient(target);

                        AccountManager manager = offline.getAccount();
                        if (manager != null) {
                            LockedAccount account = new LockedAccount(manager.getUUID());
                            LockedData data = account.getData();

                            if (data.isLocked()) {
                                if (account.unlock()) {
                                    user.send(messages.prefix() + messages.accountUnLocked(target));
                                } else {
                                    user.send(messages.prefix() + messages.accountNotLocked(target));
                                    logger.scheduleLog(Level.GRAVE, "Tried to unlock account of " + target + " but failed");
                                }
                            } else {
                                user.send(messages.prefix() + messages.accountNotLocked(target));
                            }
                        } else {
                            user.send(messages.prefix() + messages.neverPlayer(target));
                        }
                    } else {
                        user.send(messages.prefix() + messages.accountUnLock());
                    }
                } else {
                    user.send(messages.prefix() + messages.permissionError(unlockAccount()));
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            ConsoleAccount console = new ConsoleAccount();
            if (console.isRegistered()) {
                if (args.length == 2) {
                    String target = args[0];
                    String password = args[1];
                    OfflineClient offline = new OfflineClient(target);

                    if (console.validate(password)) {
                        AccountManager manager = offline.getAccount();
                        if (manager != null) {
                            LockedAccount account = new LockedAccount(manager.getUUID());
                            LockedData data = account.getData();

                            if (data.isLocked()) {
                                if (account.unlock()) {
                                    Console.send(messages.prefix() + messages.accountUnLocked(target));
                                } else {
                                    Console.send(messages.prefix() + messages.accountNotLocked(target));
                                    logger.scheduleLog(Level.GRAVE, "Tried to unlock account of " + target + " but failed");
                                }
                            } else {
                                Console.send(messages.prefix() + messages.accountNotLocked(target));
                            }
                        } else {
                            Console.send(messages.prefix() + messages.neverPlayer(target));
                        }
                    } else {
                        Console.send(messages.prefix() + messages.incorrectPassword());
                    }
                } else {
                    Console.send(messages.prefix() + messages.accountUnLock() + "&r ( remember to include the console password as last argument )");
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
        return "unlock";
    }
}
