package eu.locklogin.plugin.bungee.command;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.BruteForce;
import eu.locklogin.api.common.security.Password;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.options.BruteForceConfig;
import eu.locklogin.api.file.options.LoginConfig;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.command.util.SystemCommand;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.net.InetAddress;
import java.util.List;

import static eu.locklogin.plugin.bungee.LockLogin.*;

@SystemCommand(command = "login", aliases = {"log"})
@SuppressWarnings("unused")
public final class LoginCommand extends Command {

    private final static PluginConfiguration config = CurrentPlatform.getConfiguration();
    private final static PluginMessages messages = CurrentPlatform.getMessages();

    /**
     * Construct a new command with no permissions or aliases.
     *
     * @param name the name of this command
     */
    public LoginCommand(final String name, final List<String> aliases) {
        super(name, "", aliases.toArray(new String[0]));
    }

    /**
     * Execute this command with the specified sender and arguments.
     *
     * @param sender the executor of this command
     * @param args   arguments used to invoke this command
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            User user = new User(player);

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

                    if (!manager.isRegistered()) {
                        user.send(messages.prefix() + messages.register());
                    } else {
                        String password;

                        switch (args.length) {
                            case 0:
                                user.send(messages.prefix() + messages.login());
                                break;
                            case 1:
                                if (session.isCaptchaLogged()) {
                                    password = args[0];

                                    Password checker = new Password(password);
                                    checker.addInsecure(player.getDisplayName(), player.getName(), StringUtils.stripColor(player.getDisplayName()), StringUtils.stripColor(player.getName()));

                                    BruteForce protection = null;
                                    InetAddress ip = getIp(player.getSocketAddress());
                                    if (ip != null)
                                        protection = new BruteForce(ip);

                                    if (protection == null) {
                                        user.send(messages.prefix() + "&cError 52");
                                        return;
                                    }

                                    CryptoFactory utils = CryptoFactory.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                    if (utils.validate(Validation.ALL)) {
                                        if (!checker.isSecure()) {
                                            user.send(messages.prefix() + messages.loginInsecure());

                                            if (config.blockUnsafePasswords()) {
                                                manager.setPassword(null);

                                                SessionCheck<ProxiedPlayer> check = user.getChecker().whenComplete(user::restorePotionEffects);
                                                plugin.getProxy().getScheduler().runAsync(plugin, check);
                                                return;
                                            }
                                        }

                                        //MessageData login = DataSender.getBuilder(DataType.SESSION, CHANNEL_PLAYER, player).build();
                                        DataMessage login = DataMessage.newInstance(DataType.SESSION, Channel.ACCOUNT).addProperty("player", player.getUniqueId()).getInstance();

                                        if (utils.needsRehash(config.passwordEncryption())) {
                                            //Set the player password again to update his hash
                                            manager.setPassword(password);
                                            logger.scheduleLog(Level.INFO, "Updated password hash of {0} from {1} to {2}",
                                                    StringUtils.stripColor(player.getDisplayName()),
                                                    utils.getTokenHash().name(),
                                                    config.passwordEncryption().name());
                                        }

                                        boolean checkServer = false;
                                        if (!manager.has2FA() && !manager.hasPin()) {
                                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD,
                                                    UserAuthenticateEvent.Result.SUCCESS,
                                                    user.getModule(),
                                                    messages.logged(),
                                                    null);
                                            ModulePlugin.callEvent(event);
                                            /*MessageData pin = DataSender.getBuilder(DataType.PIN, CHANNEL_PLAYER, player)
                                                    .addProperty("pin", false).build();
                                            MessageData gauth = DataSender.getBuilder(DataType.GAUTH, CHANNEL_PLAYER, player).build();

                                            DataSender.send(player, pin);
                                            DataSender.send(player, gauth);*/

                                            BungeeSender.sender.queue(BungeeSender.serverFromPlayer(player))
                                                    .insert(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT)
                                                            .addProperty("player", player.getUniqueId())
                                                            .addProperty("pin", false).getInstance().build());

                                            BungeeSender.sender.queue(BungeeSender.serverFromPlayer(player))
                                                            .insert(DataMessage.newInstance(DataType.GAUTH, Channel.ACCOUNT)
                                                                    .addProperty("player", player.getUniqueId()).getInstance().build());

                                            session.set2FALogged(true);
                                            session.setPinLogged(true);

                                            user.send(messages.prefix() + event.getAuthMessage());
                                            checkServer = true;
                                        } else {
                                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD,
                                                    UserAuthenticateEvent.Result.SUCCESS_TEMP,
                                                    user.getModule(),
                                                    messages.logged(),
                                                    null);
                                            ModulePlugin.callEvent(event);

                                            if (manager.hasPin()) {
                                                session.setPinLogged(false);

                                                /*DataSender.send(player, DataSender.getBuilder(DataType.PIN, CHANNEL_PLAYER, player)
                                                        .addProperty("pin", true).build());*/
                                                BungeeSender.sender.queue(BungeeSender.serverFromPlayer(player))
                                                        .insert(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT)
                                                                .addProperty("player", player.getUniqueId())
                                                                .addProperty("pin", true).getInstance().build());
                                            } else {
                                                user.send(messages.prefix() + event.getAuthMessage());
                                                user.send(messages.prefix() + messages.gAuthInstructions());
                                            }
                                        }

                                        session.setLogged(true);
                                        protection.success();

                                        user.restorePotionEffects();

                                        //DataSender.send(player, login);
                                        BungeeSender.sender.queue(BungeeSender.serverFromPlayer(player))
                                                .insert(login.build());

                                        if (checkServer)
                                            user.checkServer(0);

                                        if (!manager.has2FA() && config.captchaOptions().isEnabled() && user.hasPermission(PluginPermissions.force_2fa())) {
                                            user.performCommand("2fa setup " + password);
                                        }

                                        if (!config.useVirtualID() && user.hasPermission(PluginPermissions.account())) {
                                            user.send("&cIMPORTANT!", "&7Virtual ID is disabled!", 0, 10, 0);
                                            user.send(messages.prefix() + "&dVirtual ID is disabled, this can be a security risk for everyone. Enable it in config (VirtualID: true) to dismiss this message. &4THIS MESSAGE CAN BE ONLY SEEN BY ADMINISTRATORS");
                                        }
                                    } else {
                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD,
                                                UserAuthenticateEvent.Result.ERROR,
                                                user.getModule(),
                                                messages.incorrectPassword(),
                                                null);
                                        ModulePlugin.callEvent(event);

                                        protection.fail();

                                        BruteForceConfig bruteForce = config.bruteForceOptions();
                                        LoginConfig loginConfig = config.loginOptions();

                                        if (bruteForce.getMaxTries() > 0 && protection.tries() >= bruteForce.getMaxTries()) {
                                            if (StringUtils.isNullOrEmpty(manager.getPanic())) {
                                                protection.block(bruteForce.getBlockTime());
                                                user.kick(messages.ipBlocked(protection.getBlockLeft()));
                                            } else {
                                                protection.panic(player.getUniqueId());
                                                user.kick(messages.panicMode());
                                            }
                                        } else {
                                            if (loginConfig.maxTries() > 0 && protection.tries() >= loginConfig.maxTries()) {
                                                protection.success();
                                                user.kick(event.getAuthMessage());
                                            } else {
                                                user.send(messages.prefix() + event.getAuthMessage());
                                            }
                                        }
                                    }
                                } else {
                                    if (config.captchaOptions().isEnabled()) {
                                        user.send(messages.prefix() + messages.invalidCaptcha());
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
                                        //DataSender.send(player, DataSender.getBuilder(DataType.CAPTCHA, DataSender.CHANNEL_PLAYER, player).build());
                                        BungeeSender.sender.queue(BungeeSender.serverFromPlayer(player))
                                                .insert(DataMessage.newInstance(DataType.CAPTCHA, Channel.ACCOUNT)
                                                        .addProperty("player", player.getUniqueId()).getInstance().build());
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
                        user.send(messages.prefix() + messages.alreadyLogged());
                    } else {
                        user.send(messages.prefix() + messages.gAuthenticate());
                    }
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }
    }
}
