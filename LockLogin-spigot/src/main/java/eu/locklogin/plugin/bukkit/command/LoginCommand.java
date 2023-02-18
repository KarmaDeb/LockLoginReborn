package eu.locklogin.plugin.bukkit.command;

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
import eu.locklogin.api.file.options.PasswordConfig;
import eu.locklogin.api.security.Password;
import eu.locklogin.api.common.security.client.CommandProxy;
import eu.locklogin.api.common.session.SessionCheck;
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
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.listener.data.TransientMap;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.inventory.PinInventory;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SystemCommand(command = "login", aliases = {"log"})
@SuppressWarnings("unused")
public final class LoginCommand implements CommandExecutor {

    private final static PluginConfiguration config = CurrentPlatform.getConfiguration();
    private final static PluginMessages messages = CurrentPlatform.getMessages();

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param tmpArgs    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] tmpArgs) {
        if (sender instanceof Player) {
            tryAsync(TaskTarget.COMMAND_EXECUTE, () -> {
                Player player = (Player) sender;
                User user = new User(player);

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
                                return;
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

                                        Password tmp = new Password(null);
                                        tmp.addInsecure(player.getDisplayName(), player.getName(), StringUtils.stripColor(player.getDisplayName()), StringUtils.stripColor(player.getName()));
                                        PasswordConfig passwordConfig = config.passwordConfig();
                                        Map.Entry<Boolean, String[]> rs = passwordConfig.check(password);

                                        BruteForce protection = null;
                                        if (player.getAddress() != null)
                                            protection = new BruteForce(player.getAddress().getAddress());

                                        if (protection == null) {
                                            user.send(messages.prefix() + "&cError 52");
                                            return;
                                        }

                                        if (protection.isPanicking(player.getUniqueId())) {
                                            user.send(messages.prefix() + messages.panicLogin());
                                        } else {
                                            CryptoFactory utils = CryptoFactory.getBuilder().withPassword(password).withToken(manager.getPassword()).build();

                                            if (utils.validate(Validation.ALL)) {
                                                if (!rs.getKey()) {
                                                    user.send(messages.prefix() + messages.loginInsecure());

                                                    boolean ret = false;
                                                    if (passwordConfig.block_unsafe()) {
                                                        manager.setPassword(null);

                                                        user.getChecker().cancelCheck("Logged in");
                                                        SessionCheck<Player> check = user.getChecker().whenComplete(user::restorePotionEffects);
                                                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, check);
                                                        ret = true;
                                                    } else {
                                                        if (passwordConfig.warn_unsafe()) {
                                                            for (Player online : plugin.getServer().getOnlinePlayers()) {
                                                                User staff = new User(online);
                                                                if (staff.hasPermission(PluginPermissions.warn_unsafe())) {
                                                                    staff.send(messages.prefix() + messages.passwordWarning());
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (passwordConfig.warn_unsafe()) {
                                                        for (String msg : rs.getValue())
                                                            if (msg != null)
                                                                user.send(msg);
                                                    }

                                                    if (ret) return;
                                                }

                                                if (utils.needsRehash(config.passwordEncryption())) {
                                                    //Set the player password again to update his hash
                                                    manager.setPassword(password);
                                                    logger.scheduleLog(Level.INFO, "Updated password hash of {0} from {1} to {2}",
                                                            StringUtils.stripColor(player.getDisplayName()),
                                                            utils.getTokenHash().name(),
                                                            config.passwordEncryption().name());
                                                }

                                                if (!manager.has2FA() && !manager.hasPin()) {
                                                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.SUCCESS, user.getModule(), messages.logged(), null);
                                                    ModulePlugin.callEvent(event);

                                                    user.setTempSpectator(false);

                                                    session.set2FALogged(true);
                                                    session.setPinLogged(true);

                                                    if (config.takeBack()) {
                                                        LastLocation location = new LastLocation(player);
                                                        location.teleport();
                                                    }

                                                    user.send(messages.prefix() + event.getAuthMessage());
                                                    TransientMap.apply(player);
                                                } else {
                                                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.SUCCESS_TEMP, user.getModule(), messages.logged(), null);
                                                    ModulePlugin.callEvent(event);

                                                    if (manager.hasPin()) {
                                                        session.setPinLogged(false);

                                                        PinInventory pin = new PinInventory(player);
                                                        pin.open();
                                                    } else {
                                                        user.send(messages.prefix() + event.getAuthMessage());
                                                        user.send(messages.prefix() + messages.gAuthInstructions());
                                                    }
                                                }

                                                session.setLogged(true);
                                                protection.success();

                                                if (!manager.has2FA() && config.enable2FA() && user.hasPermission(PluginPermissions.force_2fa())) {
                                                    String cmd = "2fa setup " + password;
                                                    UUID cmd_id = CommandProxy.mask(cmd, "setup", password);
                                                    String exec = CommandProxy.getCommand(cmd_id);

                                                    trySync(TaskTarget.COMMAND_FORCE, () -> player.performCommand(exec + " " + cmd_id));
                                                }

                                                if (!config.useVirtualID() && user.hasPermission(PluginPermissions.account())) {
                                                    user.send("&cIMPORTANT!", "&7Virtual ID is disabled!", 0, 10, 0);
                                                    user.send(messages.prefix() + "&dVirtual ID is disabled, this can be a security risk for everyone. Enable it in config (VirtualID: true) to dismiss this message. &5&lTHIS MESSAGE CAN BE ONLY SEEN BY ADMINISTRATORS");
                                                }
                                            } else {
                                                UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.ERROR, user.getModule(), messages.incorrectPassword(), null);
                                                ModulePlugin.callEvent(event);

                                                protection.fail();

                                                BruteForceConfig bruteForce = config.bruteForceOptions();
                                                LoginConfig loginConfig = config.loginOptions();

                                                if (bruteForce.getMaxTries() > 0 && protection.tries() >= bruteForce.getMaxTries()) {
                                                    if (manager.hasPanic()) {
                                                        protection.panic(player.getUniqueId());
                                                        user.kick(messages.panicMode());
                                                    } else {
                                                        protection.block(bruteForce.getBlockTime());
                                                        user.kick(messages.ipBlocked(bruteForce.getBlockTime()));
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

                                            String cmd = "login " + password;
                                            UUID cmd_id = CommandProxy.mask(cmd, password);
                                            String exec = CommandProxy.getCommand(cmd_id);

                                            trySync(TaskTarget.COMMAND_FORCE, () -> player.performCommand(exec + " " + cmd_id));
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
            });
        } else {
            console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }

        return false;
    }
}
