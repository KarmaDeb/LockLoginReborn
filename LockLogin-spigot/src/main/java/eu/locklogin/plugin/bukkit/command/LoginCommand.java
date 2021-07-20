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
import eu.locklogin.api.common.security.Password;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.encryption.CryptoUtil;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.options.BruteForceConfig;
import eu.locklogin.api.file.options.LoginConfig;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.inventory.PinInventory;
import eu.locklogin.plugin.bukkit.util.player.ClientVisor;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static eu.locklogin.plugin.bukkit.LockLogin.*;
import static eu.locklogin.plugin.bukkit.plugin.PluginPermission.forceFA;

@SystemCommand(command = "login")
public final class LoginCommand implements CommandExecutor {

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
                            logger.scheduleLog(Level.INFO, "Created account of player {0}", StringUtils.stripColor(player.getDisplayName()));
                        } else {
                            logger.scheduleLog(Level.GRAVE, "Couldn't create account of player {0}", StringUtils.stripColor(player.getDisplayName()));

                            user.send(messages.prefix() + properties.getProperty("could_not_create_user", "&5&oWe're sorry, but we couldn't create your account"));
                            return true;
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
                                password = args[0];

                                Password checker = new Password(password);
                                checker.addInsecure(player.getDisplayName(), player.getName(), StringUtils.stripColor(player.getDisplayName()), StringUtils.stripColor(player.getName()));

                                BruteForce protection = null;
                                if (player.getAddress() != null)
                                    protection = new BruteForce(player.getAddress().getAddress());

                                CryptoUtil utils = CryptoUtil.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                if (utils.validate()) {
                                    if (!checker.isSecure()) {
                                        user.send(messages.prefix() + messages.loginInsecure());

                                        if (config.blockUnsafePasswords()) {
                                            manager.setPassword(null);

                                            SessionCheck<Player> check = user.getChecker().whenComplete(user::restorePotionEffects);
                                            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, check);
                                            return false;
                                        }
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
                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.SUCCESS, fromPlayer(player), messages.logged(), null);
                                        ModulePlugin.callEvent(event);

                                        user.setTempSpectator(false);

                                        session.set2FALogged(true);
                                        session.setPinLogged(true);

                                        if (config.takeBack()) {
                                            LastLocation location = new LastLocation(player);
                                            location.teleport();
                                        }

                                        ClientVisor visor = new ClientVisor(player);
                                        visor.unVanish();
                                        visor.checkVanish();

                                        user.send(messages.prefix() + event.getAuthMessage());
                                    } else {
                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.SUCCESS_TEMP, fromPlayer(player), messages.logged(), null);
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
                                    if (protection != null)
                                        protection.success();

                                    if (!manager.has2FA()) {
                                        if (player.hasPermission(forceFA()))
                                            player.performCommand("2fa setup " + password);
                                    }
                                } else {
                                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.ERROR, fromPlayer(player), messages.incorrectPassword(), null);
                                    ModulePlugin.callEvent(event);

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

                                        player.performCommand("login " + password);
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
            Console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }

        return false;
    }
}
