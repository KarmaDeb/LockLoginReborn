package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.files.options.BruteForceConfig;
import ml.karmaconfigs.locklogin.api.files.options.LoginConfig;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.UserAuthenticateEvent;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.LastLocation;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.inventory.PinInventory;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.ClientVisor;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.SessionCheck;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.BruteForce;
import ml.karmaconfigs.locklogin.plugin.common.security.Password;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

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
                            logger.scheduleLog(Level.INFO, "Created account of player {0}", StringUtils.stripColor(player.getDisplayName()));
                        } else {
                            logger.scheduleLog(Level.GRAVE, "Couldn't create account of player {0}", StringUtils.stripColor(player.getDisplayName()));

                            user.send(messages.prefix() + properties.getProperty("could_not_create_user", "&5&oWe're sorry, but we couldn't create your account"));
                            return true;
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
                                checker.addInsecure(player.getDisplayName(), player.getName(), StringUtils.stripColor(player.getDisplayName()), StringUtils.stripColor(player.getName()));

                                BruteForce protection = null;
                                if (player.getAddress() != null)
                                    protection = new BruteForce(player.getAddress().getAddress());

                                if (!checker.isSecure()) {
                                    user.send(messages.prefix() + messages.loginInsecure());

                                    if (config.blockUnsafePasswords()) {
                                        manager.setPassword(null);

                                        SessionCheck check = new SessionCheck(player, null, null);
                                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, check);
                                        return false;
                                    }
                                }

                                CryptoUtil utils = CryptoUtil.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                if (utils.validate()) {
                                    if (utils.needsRehash(config.passwordEncryption())) {
                                        //Set the player password again to update his hash
                                        manager.setPassword(password);
                                        logger.scheduleLog(Level.INFO, "Updated password hash of {0} from {1} to {2}",
                                                StringUtils.stripColor(player.getDisplayName()),
                                                utils.getTokenHash().name(),
                                                config.passwordEncryption().name());
                                    }

                                    if (!manager.has2FA() && manager.getPin().replaceAll("\\s", "").isEmpty()) {
                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.SUCCESS, fromPlayer(player), messages.logged(), null);
                                        JavaModuleManager.callEvent(event);

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

                                        SessionDataContainer.setLogged(SessionDataContainer.getLogged() + 1);
                                    } else {
                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.SUCCESS_TEMP, fromPlayer(player), messages.logged(), null);
                                        JavaModuleManager.callEvent(event);

                                        if (!manager.getPin().replaceAll("\\s", "").isEmpty()) {
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

        return false;
    }
}
