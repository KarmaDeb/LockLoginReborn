package ml.karmaconfigs.locklogin.plugin.bungee.command;

import ml.karmaconfigs.api.bungee.Console;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.AccountCreatedEvent;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bungee.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.Password;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bungee.permissibles.PluginPermission.forceFA;
import static ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataSender.CHANNEL_PLAYER;

@SystemCommand(command = "register")
public final class RegisterCommand extends Command {

    /**
     * Construct a new command with no permissions or aliases.
     *
     * @param name the name of this command
     */
    public RegisterCommand(String name) {
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
        Message messages = new Message();

        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            User user = new User(player);

            PluginConfiguration config = CurrentPlatform.getConfiguration();

            ClientSession session = user.getSession();
            if (session.isValid()) {
                if (!session.isLogged()) {
                    AccountManager manager = user.getManager();
                    if (!manager.exists())
                        if (manager.create()) {
                            logger.scheduleLog(Level.INFO, "Created account of player {0}", StringUtils.stripColor(player.getDisplayName()));
                        } else {
                            logger.scheduleLog(Level.GRAVE, "Couldn't create account of player {0}", StringUtils.stripColor(player.getDisplayName()));

                            user.send(messages.prefix() + properties.getProperty("could_not_create_user", "&5&oWe're sorry, but we couldn't create your account"));
                        }

                    if (user.isRegistered()) {
                        user.send(messages.alreadyRegistered());
                    } else {
                        switch (args.length) {
                            case 2:
                                if (session.isCaptchaLogged()) {
                                    String password = args[0];
                                    String confirmation = args[1];

                                    if (password.equals(confirmation)) {
                                        Password checker = new Password(password);
                                        checker.addInsecure(player.getDisplayName(), player.getName(), StringUtils.stripColor(player.getDisplayName()), StringUtils.stripColor(player.getName()));

                                        if (!checker.isSecure()) {
                                            user.send(messages.prefix() + messages.passwordInsecure());

                                            if (config.blockUnsafePasswords()) {
                                                return;
                                            }
                                        }

                                        manager.setPassword(password);

                                        user.send(messages.prefix() + messages.registered());

                                        session.setLogged(true);

                                        if (!manager.has2FA()) {
                                            if (user.hasPermission(forceFA())) {
                                                user.performCommand("2fa setup " + password);
                                            } else {
                                                session.set2FALogged(true);
                                            }
                                        }

                                        if (manager.getPin().replaceAll("\\s", "").isEmpty())
                                            session.setPinLogged(true);

                                        AccountCreatedEvent event = new AccountCreatedEvent(fromPlayer(player), null);
                                        JavaModuleManager.callEvent(event);

                                        user.restorePotionEffects();

                                        DataSender.MessageData login = DataSender.getBuilder(DataType.SESSION, CHANNEL_PLAYER).build();
                                        DataSender.MessageData pin = DataSender.getBuilder(DataType.PIN, CHANNEL_PLAYER).addTextData("close").build();
                                        DataSender.MessageData gauth = DataSender.getBuilder(DataType.GAUTH, CHANNEL_PLAYER).build();

                                        DataSender.send(player, login);

                                        if (session.isPinLogged())
                                            DataSender.send(player, pin);
                                        if (session.is2FALogged())
                                            DataSender.send(player, gauth);

                                        SessionDataContainer.setRegistered(SessionDataContainer.getRegistered() + 1);
                                    } else {
                                        user.send(messages.prefix() + messages.registerError());
                                    }
                                } else {
                                    if (config.captchaOptions().isEnabled()) {
                                        user.send(messages.prefix() + messages.register());
                                    }
                                }
                                break;
                            case 3:
                                if (session.isCaptchaLogged()) {
                                    user.send(messages.prefix() + messages.register());
                                } else {
                                    String password = args[0];
                                    String confirmation = args[1];
                                    String captcha = args[2];

                                    if (session.getCaptcha().equals(captcha)) {
                                        session.setCaptchaLogged(true);

                                        user.performCommand("register " + password + " " + confirmation);
                                        DataSender.send(player, DataSender.getBuilder(DataType.CAPTCHA, DataSender.CHANNEL_PLAYER).build());
                                    } else {
                                        user.send(messages.prefix() + messages.invalidCaptcha());
                                    }
                                }
                                break;
                            default:
                                if (!session.isLogged()) {
                                    user.send(messages.prefix() + messages.register());
                                } else {
                                    if (session.isTempLogged()) {
                                        user.send(messages.prefix() + messages.gAuthenticate());
                                    } else {
                                        user.send(messages.prefix() + messages.alreadyRegistered());
                                    }
                                }
                                break;
                        }
                    }
                } else {
                    if (session.isTempLogged()) {
                        user.send(messages.prefix() + messages.alreadyLogged());
                    } else {
                        user.send(messages.prefix() + messages.gAuthenticate());
                    }
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            Console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }
    }
}
