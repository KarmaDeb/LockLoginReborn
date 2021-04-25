package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.util.PluginCommandType;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.Config;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

public class PinCommand extends PluginCommandType implements CommandExecutor {

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
        Config config = new Config();
        Message messages = new Message();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);
            ClientSession session = user.getSession();
            AccountManager manager = user.getManager();

            if (session.isValid()) {
                if (config.enablePin()) {
                    if (args.length == 0) {
                        user.send(messages.prefix() + messages.pinUsages());
                    } else {
                        switch (args[0].toLowerCase()) {
                            case "setup":
                                if (args.length == 2) {
                                    if (manager.getPin().replaceAll("\\s", "").isEmpty()) {
                                        String pin = args[1];

                                        manager.setPin(pin);
                                        user.send(messages.prefix() + messages.pinSet());
                                    } else {
                                        user.send(messages.prefix() + messages.alreadyPin());
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.setPin());
                                }
                                break;
                            case "remove":
                                if (args.length == 2) {
                                    if (manager.getPin().replaceAll("\\s", "").isEmpty()) {
                                        user.send(messages.prefix() + messages.noPin());
                                    } else {
                                        String current = args[1];

                                        CryptoUtil util = new CryptoUtil(current, manager.getPin());
                                        if (util.validate()) {
                                            manager.setPin(null);
                                            user.send(messages.prefix() + messages.pinReseted());
                                        } else {
                                            user.send(messages.prefix() + messages.incorrectPin());
                                        }
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.resetPin());
                                }
                                break;
                            case "change":
                                if (args.length == 3) {
                                    if (manager.getPin().replaceAll("\\s", "").isEmpty()) {
                                        user.send(messages.prefix() + messages.noPin());
                                    } else {
                                        String current = args[1];
                                        String newPin = args[2];

                                        CryptoUtil util = new CryptoUtil(current, manager.getPin());
                                        if (util.validate()) {
                                            manager.setPin(newPin);
                                            user.send(messages.prefix() + messages.pinChanged());
                                        } else {
                                            user.send(messages.prefix() + messages.incorrectPin());
                                        }
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.changePin());
                                }
                                break;
                            default:
                                user.send(messages.prefix() + messages.pinUsages());
                                break;
                        }
                    }
                } else {
                    user.send(messages.prefix() + messages.pinDisabled());
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            Console.send(plugin, properties.getProperty("only_console_pin", "&5&oThe console can't have a pin!"), Level.INFO);
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
        return "pin";
    }
}
