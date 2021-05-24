package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.AccountCreatedEvent;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.Password;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bukkit.plugin.PluginPermission.forceFA;

@SystemCommand(command = "register")
public final class RegisterCommand implements CommandExecutor {

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
                                                return false;
                                            }
                                        }

                                        manager.setPassword(password);

                                        user.send(messages.prefix() + messages.registered());

                                        session.setLogged(true);

                                        if (!manager.has2FA()) {
                                            if (player.hasPermission(forceFA())) {
                                                player.performCommand("2fa setup " + password);
                                            } else {
                                                session.set2FALogged(true);
                                            }
                                        }
                                        if (manager.getPin().replaceAll("\\s", "").isEmpty())
                                            session.setPinLogged(true);

                                        AccountCreatedEvent event = new AccountCreatedEvent(fromPlayer(player), null);
                                        JavaModuleManager.callEvent(event);
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

                                        player.performCommand("register " + password + " " + confirmation);
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
            Console.send(messages.prefix() + properties.getProperty("console_is_restricted", "&5&oFor security reasons, this command is restricted to players only"));
        }

        return false;
    }
}
