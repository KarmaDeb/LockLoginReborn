package ml.karmaconfigs.locklogin.plugin.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.velocity.Console;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.UserAuthenticateEvent;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.common.security.GoogleAuthFactory;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import ml.karmaconfigs.locklogin.plugin.velocity.command.util.BungeeLikeCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.data.ScratchCodes;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.fromPlayer;
import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.properties;

@SystemCommand(command = "2fa")
public final class GoogleAuthCommand extends BungeeLikeCommand {

    /**
     * Initialize the bungee like command
     *
     * @param label the command label
     */
    public GoogleAuthCommand(String label) {
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

                                                    String token_url = StringUtils.formatString("https://karmaconfigs.ml/qr/?{0}%20{1}?{2}", StringUtils.stripColor(player.getGameProfile().getName()), StringUtils.formatString("({0})", name), token);

                                                    HoverEvent<Component> hover = HoverEvent.showText(Component.text().content(StringUtils.toColor(properties.getProperty("command_gauth_hover", "&eClick here to scan the QR code!"))).build());
                                                    ClickEvent click = ClickEvent.openUrl(token_url);

                                                    TextComponent component = Component.text().content(StringUtils.toColor(messages.gAuthLink())).hoverEvent(hover).clickEvent(click).build();
                                                    user.send(component);

                                                    List<Integer> scratch_codes = factory.getRecoveryCodes();
                                                    user.send(messages.gAuthScratchCodes(scratch_codes));

                                                    ScratchCodes codes = new ScratchCodes(player);
                                                    codes.store(scratch_codes);

                                                    manager.setGAuth(token);
                                                } else {
                                                    user.send(messages.prefix() + messages.gAuthEnabled());
                                                }

                                                session.set2FALogged(false);
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
                                                DataSender.send(player, DataSender.getBuilder(DataType.GAUTH, DataSender.CHANNEL_PLAYER).build());

                                                session.set2FALogged(true);
                                                session.setPinLogged(true);

                                                UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2, UserAuthenticateEvent.Result.SUCCESS, fromPlayer(player), messages.gAuthCorrect(), null);
                                                JavaModuleManager.callEvent(event);

                                                user.send(messages.prefix() + event.getAuthMessage());

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
    }
}
