package ml.karmaconfigs.locklogin.plugin.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.velocity.Console;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.files.options.BruteForceConfig;
import ml.karmaconfigs.locklogin.api.files.options.LoginConfig;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.UserAuthenticateEvent;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.common.security.BruteForce;
import ml.karmaconfigs.locklogin.plugin.common.security.Password;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import ml.karmaconfigs.locklogin.plugin.velocity.command.util.BungeeLikeCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.SessionCheck;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;

import java.net.InetAddress;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender.CHANNEL_PLAYER;
import static ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender.MessageData;

@SystemCommand(command = "login")
public final class LoginCommand extends BungeeLikeCommand {

    /**
     * Initialize the bungee like command
     *
     * @param label the command label
     */
    public LoginCommand(String label) {
        super(label);
    }

    /**
     * Execute this command with the specified sender and arguments.
     *
     * @param sender the executor of this command
     * @param args   arguments used to invoke this command
     */
    @Override
    public void execute(CommandSource sender, String[] args) {
        Message messages = new Message();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            PluginConfiguration config = CurrentPlatform.getConfiguration();

            ClientSession session = user.getSession();
            if (session.isValid()) {
                if (!session.isLogged()) {
                    AccountManager manager = user.getManager();
                    if (!manager.exists())
                        if (manager.create()) {
                            logger.scheduleLog(Level.INFO, "Created account of player {0}", StringUtils.stripColor(player.getUsername()));
                        } else {
                            logger.scheduleLog(Level.GRAVE, "Couldn't create account of player {0}", StringUtils.stripColor(player.getUsername()));

                            user.send(messages.prefix() + properties.getProperty("could_not_create_user", "&5&oWe're sorry, but we couldn't create your account"));
                        }

                    if (!user.isRegistered()) {
                        user.send(messages.prefix() + messages.register());
                    } else {
                        String password;

                        switch (args.length) {
                            case 0:
                                user.send(messages.prefix() + messages.login());
                                break;
                            case 1:
                                password = args[0];

                                Password checker = new Password(password);
                                checker.addInsecure(player.getUsername(), player.getGameProfile().getName(), StringUtils.stripColor(player.getUsername()), StringUtils.stripColor(player.getGameProfile().getName()));

                                BruteForce protection = null;
                                InetAddress ip = player.getRemoteAddress().getAddress();
                                if (ip != null)
                                    protection = new BruteForce(ip);

                                if (!checker.isSecure()) {
                                    user.send(messages.prefix() + messages.loginInsecure());

                                    if (config.blockUnsafePasswords()) {
                                        manager.setPassword(null);

                                        SessionCheck check = new SessionCheck(player, null, null);
                                        server.getScheduler().buildTask(plugin, check).schedule();
                                        return;
                                    }
                                }

                                CryptoUtil utils = CryptoUtil.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                if (utils.validate()) {
                                    MessageData login = DataSender.getBuilder(DataType.SESSION, CHANNEL_PLAYER, player).build();

                                    if (utils.needsRehash(config.passwordEncryption())) {
                                        //Set the player password again to update his hash
                                        manager.setPassword(password);
                                        logger.scheduleLog(Level.INFO, "Updated password hash of {0} from {1} to {2}",
                                                StringUtils.stripColor(player.getUsername()),
                                                utils.getTokenHash().name(),
                                                config.passwordEncryption().name());
                                    }

                                    if (!manager.has2FA() && manager.getPin().replaceAll("\\s", "").isEmpty()) {
                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.SUCCESS, fromPlayer(player), messages.logged(), null);
                                        JavaModuleManager.callEvent(event);
                                        MessageData pin = DataSender.getBuilder(DataType.PIN, CHANNEL_PLAYER, player).addTextData("close").build();
                                        MessageData gauth = DataSender.getBuilder(DataType.GAUTH, CHANNEL_PLAYER, player).build();

                                        DataSender.send(player, pin);
                                        DataSender.send(player, gauth);

                                        session.set2FALogged(true);
                                        session.setPinLogged(true);

                                        user.send(messages.prefix() + event.getAuthMessage());

                                        SessionDataContainer.setLogged(SessionDataContainer.getLogged() + 1);
                                    } else {
                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.SUCCESS_TEMP, fromPlayer(player), messages.logged(), null);
                                        JavaModuleManager.callEvent(event);

                                        if (!manager.getPin().replaceAll("\\s", "").isEmpty()) {
                                            session.setPinLogged(false);

                                            DataSender.send(player, DataSender.getBuilder(DataType.PIN, CHANNEL_PLAYER, player).addTextData("open").build());
                                        } else {
                                            user.send(messages.prefix() + event.getAuthMessage());
                                            user.send(messages.prefix() + messages.gAuthInstructions());
                                        }
                                    }

                                    session.setLogged(true);
                                    if (protection != null)
                                        protection.success();

                                    user.restorePotionEffects();

                                    DataSender.send(player, login);
                                } else {
                                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.ERROR, fromPlayer(player), messages.incorrectPassword(), null);
                                    JavaModuleManager.callEvent(event);

                                    if (protection != null) {
                                        protection.fail();

                                        BruteForceConfig bruteForce = config.bruteForceOptions();
                                        LoginConfig loginConfig = config.loginOptions();

                                        if (bruteForce.getMaxTries() > 0 && protection.tries() >= bruteForce.getMaxTries()) {
                                            protection.block(bruteForce.getBlockTime());
                                            user.kick(messages.ipBlocked(protection.getBlockLeft()));
                                        } else {
                                            if (loginConfig.maxTries() > 0 && protection.tries() >= loginConfig.maxTries()) {
                                                protection.success();
                                                user.kick(event.getAuthMessage());
                                            } else {
                                                user.send(messages.prefix() + event.getAuthMessage());
                                            }
                                        }
                                    } else {
                                        user.send(messages.prefix() + event.getAuthMessage());
                                    }
                                }
                                break;
                            case 2:
                                if (session.isCaptchaLogged()) {
                                    user.send(messages.prefix() + messages.login());
                                } else {
                                    password = args[0];
                                    String captcha = args[1];

                                    if (session.getCaptcha().equals(captcha)) {
                                        session.setCaptchaLogged(true);

                                        user.performCommand("login " + password);
                                        DataSender.send(player, DataSender.getBuilder(DataType.CAPTCHA, DataSender.CHANNEL_PLAYER, player).build());
                                    } else {
                                        user.send(messages.prefix() + messages.invalidCaptcha());
                                    }
                                }
                                break;
                            default:
                                if (!session.isLogged()) {
                                    user.send(messages.prefix() + messages.login());
                                } else {
                                    if (session.isTempLogged()) {
                                        user.send(messages.prefix() + messages.gAuthenticate());
                                    } else {
                                        user.send(messages.prefix() + messages.alreadyLogged());
                                    }
                                }
                                break;
                        }
                    }
                } else {
                    if (session.isTempLogged()) {
                        user.send(messages.prefix() + messages.gAuthenticate());
                    } else {
                        user.send(messages.prefix() + messages.alreadyLogged());
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
