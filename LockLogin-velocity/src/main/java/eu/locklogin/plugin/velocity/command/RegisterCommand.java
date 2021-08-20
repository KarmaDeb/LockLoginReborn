package eu.locklogin.plugin.velocity.command;

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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.Password;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.user.AccountCreatedEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.command.util.BungeeLikeCommand;
import eu.locklogin.plugin.velocity.command.util.SystemCommand;
import eu.locklogin.plugin.velocity.permissibles.PluginPermission;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;

import static eu.locklogin.plugin.velocity.LockLogin.*;
import static eu.locklogin.plugin.velocity.plugin.sender.DataSender.CHANNEL_PLAYER;

@SystemCommand(command = "register")
public final class RegisterCommand extends BungeeLikeCommand {

    /**
     * Initialize the bungee like command
     *
     * @param label the command label
     */
    public RegisterCommand(String label) {
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
        PluginMessages messages = CurrentPlatform.getMessages();

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
                                        checker.addInsecure(player.getUsername(), player.getGameProfile().getName(), StringUtils.stripColor(player.getUsername()), StringUtils.stripColor(player.getGameProfile().getName()));

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
                                            if (user.hasPermission(PluginPermission.forceFA())) {
                                                user.performCommand("2fa setup " + password);
                                            } else {
                                                session.set2FALogged(true);
                                            }
                                        }

                                        if (!manager.hasPin())
                                            session.setPinLogged(true);

                                        AccountCreatedEvent event = new AccountCreatedEvent(fromPlayer(player), null);
                                        ModulePlugin.callEvent(event);

                                        user.restorePotionEffects();

                                        DataSender.MessageData login = DataSender.getBuilder(DataType.SESSION, CHANNEL_PLAYER, player).build();
                                        DataSender.MessageData pin = DataSender.getBuilder(DataType.PIN, CHANNEL_PLAYER, player).addTextData("close").build();
                                        DataSender.MessageData gauth = DataSender.getBuilder(DataType.GAUTH, CHANNEL_PLAYER, player).build();

                                        DataSender.send(player, login);

                                        if (session.isPinLogged())
                                            DataSender.send(player, pin);
                                        if (session.is2FALogged())
                                            DataSender.send(player, gauth);

                                        user.checkServer(0);
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
                                        DataSender.send(player, DataSender.getBuilder(DataType.CAPTCHA, DataSender.CHANNEL_PLAYER, player).build());
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
            console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }
    }
}
