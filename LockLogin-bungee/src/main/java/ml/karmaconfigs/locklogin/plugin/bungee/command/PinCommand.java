package ml.karmaconfigs.locklogin.plugin.bungee.command;

import ml.karmaconfigs.api.bungee.Console;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bungee.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataType;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.plugin;
import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.properties;

@SystemCommand(command = "pin")
public class PinCommand extends Command {

    /**
     * Construct a new command with no permissions or aliases.
     *
     * @param name the name of this command
     */
    public PinCommand(String name) {
        super(name);
    }

    /**
     * Execute this command with the specified sender and arguments.
     *
     * @param sender the executor of this command
     * @param args   arguments used to invoke this command
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        Message messages = new Message();

        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            User user = new User(player);
            ClientSession session = user.getSession();
            AccountManager manager = user.getManager();

            if (session.isValid()) {
                if (config.enablePin()) {
                    if (session.isCaptchaLogged() && session.isLogged() && session.isTempLogged()) {
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

                                            session.setPinLogged(false);

                                            DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER).addTextData("open").build());
                                            SessionDataContainer.setLogged(SessionDataContainer.getLogged() - 1);
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

                                            CryptoUtil util = CryptoUtil.getBuilder().withPassword(current).withToken(manager.getPin()).build();
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

                                            CryptoUtil util = CryptoUtil.getBuilder().withPassword(current).withToken(manager.getPin()).build();
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
    }
}
