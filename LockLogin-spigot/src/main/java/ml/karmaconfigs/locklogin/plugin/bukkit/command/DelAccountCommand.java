package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.util.PluginCommandType;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.ConsoleAccount;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.Config;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.lock.LockedAccount;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.SessionCheck;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bukkit.permission.PluginPermission.*;

public final class DelAccountCommand extends PluginCommandType implements CommandExecutor {

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
        Config config = new Config();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            if (user.getSession().isValid()) {
                switch (args.length) {
                    case 1:
                        if (player.hasPermission(account())) {
                            String target = args[0];
                            Player online = plugin.getServer().getPlayer(target);
                            OfflineClient offline = new OfflineClient(target);

                            AccountManager manager = offline.getAccount();
                            if (manager != null) {
                                LockedAccount account = new LockedAccount(manager.getUUID());

                                manager.set2FA(false);
                                manager.setGAuth(null);
                                manager.setPassword(null);
                                manager.setPin(null);

                                user.send(messages.prefix() + messages.forcedDelAccountAdmin(target));

                                if (online != null) {
                                    User onlineUser = new User(online);
                                    onlineUser.kick(messages.forcedDelAccount(StringUtils.stripColor(player.getDisplayName())));
                                }

                                account.lock(StringUtils.stripColor(player.getDisplayName()));
                            } else {
                                user.send(messages.prefix() + messages.neverPlayer(target));
                            }
                        } else {
                            user.send(messages.prefix() + messages.permissionError(delAccount()));
                        }
                        break;
                    case 2:
                        String password = args[0];
                        String confirmation = args[1];

                        if (password.equals(confirmation)) {
                            CryptoUtil util = new CryptoUtil(password, user.getManager().getPassword());
                            if (util.validate()) {
                                user.send(messages.prefix() + messages.accountDeleted());

                                //Completely restart the client session
                                user.getSession().setPinLogged(false);
                                user.getSession().set2FALogged(false);
                                user.getSession().setLogged(false);
                                user.getSession().invalidate();
                                user.getSession().validate();

                                SessionCheck check = new SessionCheck(player, null, null);
                                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, check);
                            } else {
                                user.send(messages.prefix() + messages.incorrectPassword());
                            }
                        } else {
                            user.send(messages.prefix() + messages.delAccountMatch());
                        }
                        break;
                    default:
                        user.send(messages.prefix() + messages.delAccountUsage(label));
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

                    if (console.validate(password)) {
                        Player online = plugin.getServer().getPlayer(target);
                        OfflineClient offline = new OfflineClient(target);

                        AccountManager manager = offline.getAccount();
                        if (manager != null) {
                            LockedAccount account = new LockedAccount(manager.getUUID());

                            manager.set2FA(false);
                            manager.setGAuth(null);
                            manager.setPassword(null);
                            manager.setPin(null);

                            Console.send(messages.prefix() + messages.forcedDelAccountAdmin(target));

                            if (online != null) {
                                User onlineUser = new User(online);
                                onlineUser.kick(messages.forcedDelAccount(StringUtils.stripColor(config.serverName())));
                            }

                            account.lock(StringUtils.stripColor(config.serverName()));
                        } else {
                            Console.send(messages.prefix() + messages.neverPlayer(target));
                        }
                    } else {
                        Console.send(messages.prefix() + messages.incorrectPassword());
                    }
                } else {
                    Console.send(messages.prefix() + messages.delAccountUsage(label) + "&r ( remember to include the console password as last argument )");
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
        return "delaccount";
    }
}
