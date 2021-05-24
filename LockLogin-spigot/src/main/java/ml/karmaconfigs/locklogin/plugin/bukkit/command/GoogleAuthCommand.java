package ml.karmaconfigs.locklogin.plugin.bukkit.command;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.UserAuthenticateEvent;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.LastLocation;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.ScratchCodes;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.GoogleAuthFactory;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.ComponentFactory;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.fromPlayer;
import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.properties;

@SystemCommand(command = "2fa")
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
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        Message messages = new Message();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            ClientSession session = user.getSession();
            AccountManager manager = user.getManager();
            if (session.isValid()) {
                if (config.enable2FA()) {
                    if (args.length == 0) {
                        user.send(messages.prefix() + messages.gAuthUsages());
                    } else {
                        switch (args[0].toLowerCase()) {
                            case "setup":
                                if (session.isCaptchaLogged() && session.isLogged() && session.isPinLogged()) {
                                    if (args.length == 2) {
                                        if (manager.has2FA()) {
                                            user.send(messages.prefix() + messages.gAuthSetupAlready());
                                        } else {
                                            String password = args[1];

                                            CryptoUtil util = CryptoUtil.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                            if (util.validate()) {
                                                String token = manager.getGAuth();

                                                if (token.replaceAll("\\s", "").isEmpty()) {
                                                    GoogleAuthFactory factory = user.getTokenFactory();
                                                    token = factory.generateToken();

                                                    user.send(messages.prefix() + messages.gAuthInstructions());

                                                    String name = config.serverName();
                                                    if (name.replaceAll("\\s", "").isEmpty())
                                                        name = "LockLogin";

                                                    String token_url = StringUtils.formatString("https://karmaconfigs.ml/qr/?{0}%20{1}?{2}", StringUtils.stripColor(player.getDisplayName()), StringUtils.formatString("({0})", name), token);

                                                    ComponentFactory c_factory = new ComponentFactory(messages.gAuthLink()).hover(properties.getProperty("command_gauth_hover", "&eClick here to scan the QR code!")).click(ClickEvent.Action.OPEN_URL, token_url);
                                                    user.send(c_factory.get());

                                                    List<Integer> scratch_codes = factory.getRecoveryCodes();
                                                    user.send(messages.gAuthScratchCodes(scratch_codes));

                                                    ScratchCodes codes = new ScratchCodes(player);
                                                    codes.store(scratch_codes);

                                                    manager.setGAuth(token);
                                                } else {
                                                    user.send(messages.prefix() + messages.gAuthEnabled());
                                                }

                                                session.set2FALogged(true);
                                                manager.set2FA(true);
                                            } else {
                                                user.send(messages.prefix() + messages.gAuthToggleError());
                                            }
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.gAuthSetupUsage());
                                    }
                                }
                                break;
                            case "remove":
                                if (session.isCaptchaLogged() && session.isLogged() && session.isTempLogged()) {
                                    if (args.length >= 3) {
                                        if (manager.has2FA()) {
                                            String password = args[1];

                                            StringBuilder codeBuilder = new StringBuilder();
                                            for (int i = 2; i < args.length; i++)
                                                codeBuilder.append(args[i]);

                                            try {
                                                int code = Integer.parseInt(codeBuilder.toString());

                                                CryptoUtil util = CryptoUtil.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                                GoogleAuthFactory factory = user.getTokenFactory();

                                                if (util.validate() && factory.validate(manager.getGAuth(), code)) {
                                                    manager.set2FA(false);
                                                    manager.setGAuth(null);

                                                    user.send(messages.prefix() + messages.gAuthDisabled());
                                                } else {
                                                    user.send(messages.prefix() + messages.gAuthToggleError());
                                                }
                                            } catch (Throwable ex) {
                                                user.send(messages.prefix() + messages.gAuthIncorrect());
                                            }
                                        } else {
                                            user.send(messages.prefix() + messages.gAuthNotEnabled());
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.gAuthRemoveUsage());
                                    }
                                }
                                break;
                            default:
                                if (manager.has2FA()) {
                                    if (!session.is2FALogged()) {
                                        StringBuilder codeBuilder = new StringBuilder();
                                        for (String arg : args) codeBuilder.append(arg);

                                        try {
                                            int code = Integer.parseInt(codeBuilder.toString());

                                            GoogleAuthFactory factory = user.getTokenFactory();
                                            ScratchCodes codes = new ScratchCodes(player);

                                            if (factory.validate(manager.getGAuth(), code) || codes.validate(code)) {
                                                user.setTempSpectator(false);

                                                session.set2FALogged(true);
                                                session.setPinLogged(true);

                                                UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2, UserAuthenticateEvent.Result.SUCCESS, fromPlayer(player), messages.gAuthCorrect(), null);
                                                JavaModuleManager.callEvent(event);

                                                user.send(messages.prefix() + event.getAuthMessage());

                                                if (config.takeBack()) {
                                                    LastLocation location = new LastLocation(player);
                                                    location.teleport();
                                                }

                                                if (codes.needsNew()) {
                                                    List<Integer> newCodes = GoogleAuthFactory.ScratchGenerator.generate();
                                                    user.send(messages.gAuthScratchCodes(newCodes));

                                                    codes.store(newCodes);
                                                }

                                                SessionDataContainer.setLogged(SessionDataContainer.getLogged() + 1);
                                            } else {
                                                UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2, UserAuthenticateEvent.Result.FAILED, fromPlayer(player), messages.gAuthIncorrect(), null);
                                                JavaModuleManager.callEvent(event);

                                                user.send(messages.prefix() + event.getAuthMessage());
                                            }
                                        } catch (Throwable ex) {
                                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2, UserAuthenticateEvent.Result.ERROR, fromPlayer(player), messages.gAuthIncorrect(), null);
                                            JavaModuleManager.callEvent(event);

                                            user.send(messages.prefix() + event.getAuthMessage());
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.gAuthAlready());
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.gAuthNotEnabled());
                                }
                                break;
                        }
                    }
                } else {
                    user.send(messages.prefix() + messages.gAuthServerDisabled());
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
