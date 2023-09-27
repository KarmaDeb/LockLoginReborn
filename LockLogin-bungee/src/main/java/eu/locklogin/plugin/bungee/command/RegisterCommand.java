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
import eu.locklogin.api.common.security.client.CommandProxy;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.options.PasswordConfig;
import eu.locklogin.api.module.plugin.api.event.user.AccountCreatedEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.security.Password;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.command.util.SystemCommand;
import eu.locklogin.plugin.bungee.plugin.Manager;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.locklogin.plugin.bungee.LockLogin.*;

@SystemCommand(command = "register", aliases = {"reg"})
@SuppressWarnings("unused")
public final class RegisterCommand extends Command {

    /**
     * Construct a new command with no permissions or aliases.
     *
     * @param name the name of this command
     */
    public RegisterCommand(final String name, final List<String> aliases) {
        super(name, "", aliases.toArray(new String[0]));
    }

    /**
     * Execute this command with the specified sender and arguments.
     *
     * @param sender the executor of this command
     * @param tmpArgs   arguments used to invoke this command
     */
    @Override
    public void execute(CommandSender sender, String[] tmpArgs) {
        PluginMessages messages = CurrentPlatform.getMessages();

        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            User user = new User(player);

            PluginConfiguration config = CurrentPlatform.getConfiguration();

            ClientSession session = user.getSession();
            if (session.isValid()) {
                boolean validated = false;

                String[] args = new String[0];
                if (tmpArgs.length >= 1) {
                    String last_arg = tmpArgs[tmpArgs.length - 1];
                    try {
                        UUID command_id = UUID.fromString(last_arg);
                        args = CommandProxy.getArguments(command_id);
                        validated = true;
                    } catch (Throwable ignored) {}
                }

                if (!validated) {
                    if (!session.isLogged()) {
                        user.send(messages.prefix() + messages.register());
                    } else {
                        if (session.isTempLogged()) {
                            user.send(messages.prefix() + messages.gAuthenticate());
                        } else {
                            user.send(messages.prefix() + messages.alreadyRegistered());
                        }
                    }

                    return;
                }

                if (!session.isLogged()) {
                    AccountManager manager = user.getManager();
                    if (!manager.exists())
                        if (manager.create()) {
                            logger.scheduleLog(Level.INFO, "Created account of player {0}", StringUtils.stripColor(player.getDisplayName()));
                        } else {
                            logger.scheduleLog(Level.GRAVE, "Couldn't create account of player {0}", StringUtils.stripColor(player.getDisplayName()));

                            user.send(messages.prefix() + properties.getProperty("could_not_create_user", "&5&oWe're sorry, but we couldn't create your account"));
                        }

                    if (manager.isRegistered()) {
                        user.send(messages.alreadyRegistered());
                    } else {
                        switch (args.length) {
                            case 2:
                                if (session.isCaptchaLogged()) {
                                    String password = args[0];
                                    String confirmation = args[1];

                                    if (password.equals(confirmation)) {
                                        Password tmp = new Password(null);
                                        tmp.addInsecure(player.getDisplayName(), player.getName(), StringUtils.stripColor(player.getDisplayName()), StringUtils.stripColor(player.getName()));
                                        PasswordConfig passwordConfig = config.passwordConfig();
                                        Map.Entry<Boolean, String[]> rs = passwordConfig.check(password);

                                        if (!rs.getKey()) {
                                            boolean ret = false;
                                            user.send(messages.prefix() + messages.passwordInsecure());

                                            if (passwordConfig.block_unsafe()) {
                                                ret = true;
                                            } else {
                                                if (passwordConfig.warn_unsafe()) {
                                                    for (ProxiedPlayer online : plugin.getProxy().getPlayers()) {
                                                        User staff = new User(online);
                                                        if (staff.hasPermission(PluginPermissions.warn_password())) {
                                                            staff.send(messages.prefix() + messages.passwordWarning());
                                                        }
                                                    }
                                                }
                                            }

                                            if (passwordConfig.warn_unsafe()) {
                                                for (String msg : rs.getValue()) {
                                                    if (msg != null)
                                                        user.send(msg);
                                                }
                                            }

                                            if (ret) return;
                                        }

                                        manager.setPassword(password);
                                        //PluginMetricsService.register(player.getName());

                                        user.send(messages.prefix() + messages.registered());

                                        session.setLogged(true);

                                        if (!manager.has2FA() && config.captchaOptions().isEnabled() && user.hasPermission(PluginPermissions.force_2fa())) {
                                            String cmd = "2fa setup " + password;
                                            UUID cmd_id = CommandProxy.mask(cmd, "setup", password);
                                            String exec = CommandProxy.getCommand(cmd_id);

                                            user.performCommand(exec);
                                        } else {
                                            session.set2FALogged(true);
                                        }

                                        if (!manager.hasPin())
                                            session.setPinLogged(true);

                                        Event event = new AccountCreatedEvent(user.getModule(), null);
                                        ModulePlugin.callEvent(event);

                                        user.restorePotionEffects();

                                        if (session.isPinLogged()) {
                                            Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.SESSION, Channel.ACCOUNT, player)
                                                    .getInstance().build());
                                        }

                                        if (session.is2FALogged()) {
                                            Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT, player)
                                                    .addProperty("pin", false).getInstance().build());
                                        }

                                        Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.GAUTH, Channel.ACCOUNT, player)
                                                .getInstance().build());

                                        user.checkServer(0, true);

                                        if (!config.useVirtualID() && user.hasPermission(PluginPermissions.account())) {
                                            user.send("&cIMPORTANT!", "&7Virtual ID is disabled!", 0, 10, 0);
                                            user.send(messages.prefix() + "&dVirtual ID is disabled, this can be a security risk for everyone. Enable it in config (VirtualID: true) to dismiss this message. &4THIS MESSAGE CAN BE ONLY SEEN BY ADMINISTRATORS");
                                        }
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

                                        String cmd = "register " + password + " " + confirmation;
                                        UUID cmd_id = CommandProxy.mask(cmd, password, confirmation);
                                        String exec = CommandProxy.getCommand(cmd_id);
                                        user.performCommand(exec);

                                        Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.CAPTCHA, Channel.ACCOUNT, player)
                                                .getInstance().build());
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
