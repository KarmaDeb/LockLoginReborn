package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.LockLoginListener;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.event.user.UserAuthenticateEvent;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.util.PluginCommandType;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.Config;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.LastLocation;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.SessionCheck;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.Password;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.logger;

public final class LoginCommand extends PluginCommandType implements CommandExecutor {

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
        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            Message messages = new Message();
            Config config = new Config();

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

                                if (checker.isSecure()) {
                                    CryptoUtil util = new CryptoUtil(password, manager.getPassword());
                                    if (util.validate()) {
                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.SUCCESS, player, messages.logged(), null);
                                        LockLoginListener.callEvent(event);

                                        session.setLogged(true);

                                        if (config.takeBack()) {
                                            LastLocation location = new LastLocation(player);
                                            location.teleport();
                                        }

                                        user.send(messages.prefix() + event.getAuthMessage());
                                    } else {
                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PASSWORD, UserAuthenticateEvent.Result.ERROR, player, messages.logged(), null);
                                        LockLoginListener.callEvent(event);

                                        user.send(messages.prefix() + messages.incorrectPassword());
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.loginInsecure());

                                    manager.setPassword(null);

                                    SessionCheck check = new SessionCheck(player, null, null);
                                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, check);
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
            Console.send(plugin, properties.getProperty("only_console_login", "&5&oYou can't login as the console!"), Level.INFO);
        }

        return false;
    }

    /**
     * Get the plugin command name
     *
     * @return the plugin command
     */
    @Override
    public String command() {
        return "login";
    }
}
