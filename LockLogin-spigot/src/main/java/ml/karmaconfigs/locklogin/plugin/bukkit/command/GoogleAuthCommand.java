package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.LockLoginListener;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.event.user.UserAuthenticateEvent;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.Config;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.ScratchCodes;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.GoogleAuthFactory;
import ml.karmaconfigs.locklogin.plugin.common.utils.ComponentFactory;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.plugin;
import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.properties;

public final class GoogleAuthCommand implements CommandExecutor {

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
        Config config = new Config();
        Message message = new Message();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            ClientSession session = user.getSession();
            AccountManager manager = user.getManager();
            if (session.isValid()) {
                if (config.enable2FA()) {
                    if (args.length == 0) {
                        user.send(message.prefix() + message.gAuthUsages());
                    } else {
                        switch (args[0].toLowerCase()) {
                            case "setup":
                                if (args.length == 2) {
                                    if (manager.has2FA()) {
                                        user.send(message.prefix() + message.gAuthSetupAlready());
                                    } else {
                                        String password = args[1];

                                        CryptoUtil util = new CryptoUtil(password, manager.getPassword());
                                        if (util.validate()) {
                                            String token = manager.getGAuth();

                                            if (token.replaceAll("\\s", "").isEmpty()) {
                                                GoogleAuthFactory factory = user.getTokenFactory();
                                                token = factory.generateToken();

                                                user.send(message.prefix() + message.gAuthInstructions());

                                                String name = config.serverName();
                                                if (name.replaceAll("\\s", "").isEmpty())
                                                    name = "LockLogin";

                                                String token_url = StringUtils.formatString("https://karmaconfigs.ml/qr/?{0}%20{1}?{2}", StringUtils.stripColor(player.getDisplayName()), StringUtils.formatString("({0})", name), token);

                                                ComponentFactory c_factory = new ComponentFactory(message.gAuthLink()).hover(properties.getProperty("command_gauth_hover", "&eClick here to scan the QR code!")).click(ClickEvent.Action.OPEN_URL, token_url);
                                                user.send(c_factory.get());

                                                List<Integer> scratch_codes = factory.getRecoveryCodes();
                                                user.send(message.gAuthScratchCodes(scratch_codes));

                                                ScratchCodes codes = new ScratchCodes(player);
                                                codes.store(scratch_codes);

                                                manager.setGAuth(token);
                                            } else {
                                                user.send(message.prefix() + message.gAuthEnabled());
                                            }

                                            session.setTempLogged(false);
                                            manager.set2FA(true);
                                        } else {
                                            user.send(message.prefix() + message.gAuthToggleError());
                                        }
                                    }
                                } else {
                                    user.send(message.prefix() + message.gAuthSetupUsage());
                                }
                                break;
                            case "remove":
                                if (args.length >= 3) {
                                    if (manager.has2FA()) {
                                        String password = args[0];

                                        StringBuilder codeBuilder = new StringBuilder();
                                        for (int i = 1; i < args.length; i++)
                                            codeBuilder.append(args[i]);

                                        try {
                                            int code = Integer.parseInt(codeBuilder.toString());

                                            CryptoUtil util = new CryptoUtil(password, manager.getPassword());
                                            GoogleAuthFactory factory = user.getTokenFactory();
                                            if (util.validate() && factory.validate(manager.getGAuth(), code)) {
                                                manager.set2FA(false);

                                                user.send(message.prefix() + message.gAuthDisabled());
                                            } else {
                                                user.send(message.prefix() + message.gAuthToggleError());
                                            }
                                        } catch (Throwable ex) {
                                            user.send(message.prefix() + message.gAuthIncorrect());
                                        }
                                    } else {
                                        user.send(message.prefix() + message.gAuthNotEnabled());
                                    }
                                } else {
                                    user.send(message.prefix() + message.gAuthRemoveUsage());
                                }
                                break;
                            default:
                                if (manager.has2FA()) {
                                    StringBuilder codeBuilder = new StringBuilder();
                                    for (String arg : args) codeBuilder.append(arg);

                                    try {
                                        int code = Integer.parseInt(codeBuilder.toString());

                                        GoogleAuthFactory factory = user.getTokenFactory();
                                        ScratchCodes codes = new ScratchCodes(player);

                                        if (factory.validate(manager.getGAuth(), code) || codes.validate(code)) {
                                            session.setTempLogged(false);

                                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2, UserAuthenticateEvent.Result.SUCCESS, player, message.gAuthCorrect(), null);
                                            LockLoginListener.callEvent(event);

                                            user.send(message.prefix() + event.getAuthMessage());

                                            if (codes.needsNew()) {
                                                List<Integer> newCodes = GoogleAuthFactory.ScratchGenerator.generate();
                                                user.send(message.gAuthScratchCodes(newCodes));

                                                codes.store(newCodes);
                                            }
                                        } else {
                                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2, UserAuthenticateEvent.Result.FAILED, player, message.gAuthIncorrect(), null);
                                            LockLoginListener.callEvent(event);

                                            user.send(message.prefix() + event.getAuthMessage());
                                        }
                                    } catch (Throwable ex) {
                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2, UserAuthenticateEvent.Result.ERROR, player, message.gAuthIncorrect(), null);
                                        LockLoginListener.callEvent(event);

                                        user.send(message.prefix() + event.getAuthMessage());
                                    }
                                } else {
                                    user.send(message.prefix() + message.gAuthNotEnabled());
                                }
                                break;
                        }
                    }
                } else {
                    user.send(message.prefix() + message.gAuthServerDisabled());
                }
            } else {
                user.send(message.prefix() + properties.getProperty("session_not_valid", "&cYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            Console.send(plugin, "", Level.INFO);
        }
        return false;
    }
}
