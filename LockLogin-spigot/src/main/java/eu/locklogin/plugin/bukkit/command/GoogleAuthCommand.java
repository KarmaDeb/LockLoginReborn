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
import eu.locklogin.api.account.ScratchCodes;
import eu.locklogin.api.common.security.client.CommandProxy;
import eu.locklogin.api.common.security.google.GoogleAuthFactory;
import eu.locklogin.api.common.utils.plugin.ComponentFactory;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.listener.data.TransientMap;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.url.URLUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.List;
import java.util.UUID;

import static eu.locklogin.plugin.bukkit.LockLogin.console;
import static eu.locklogin.plugin.bukkit.LockLogin.properties;

@SystemCommand(command = "2fa")
@SuppressWarnings("unused")
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
     * @param tmpArgs    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] tmpArgs) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PluginMessages messages = CurrentPlatform.getMessages();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);

            ClientSession session = user.getSession();
            AccountManager manager = user.getManager();
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

                    return false;
                }

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

                                            CryptoFactory util = CryptoFactory.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                            if (util.validate(Validation.ALL)) {
                                                String token = manager.getGAuth();

                                                if (token.replaceAll("\\s", "").isEmpty()) {
                                                    GoogleAuthFactory factory = user.getTokenFactory();
                                                    token = factory.generateToken();

                                                    user.send(messages.prefix() + messages.gAuthInstructions());

                                                    String name = config.serverName();
                                                    if (name.replaceAll("\\s", "").isEmpty())
                                                        name = "LockLogin";

                                                    String[] tries = new String[]{
                                                            "https://karmaconfigs.ml/",
                                                            "https://karmadev.es/",
                                                            "https://karmarepo.ml/",
                                                            "https://backup.karmaconfigs.ml/",
                                                            "https://backup.karmadev.es/",
                                                            "https://backup.karmarepo.ml/"
                                                    };
                                                    URL working = URLUtils.getOrBackup(tries);

                                                    String token_url = StringUtils.formatString(working + "locklogin/qr/?{0}%20{1}?{2}",
                                                            /*{9}*/StringUtils.stripColor(player.getDisplayName()),
                                                            /*{1}*/StringUtils.formatString("({0})", name.replaceAll("\\s", "%20")),
                                                            /*{2}*/token);

                                                    ComponentFactory c_factory = new ComponentFactory(messages.gAuthLink())
                                                            .hover(properties.getProperty("command_gauth_hover", "&eClick here to scan the QR code!"))
                                                            .click(ClickEvent.Action.OPEN_URL, token_url.replaceAll("\\s", "%20"));
                                                    user.send(c_factory.get());

                                                    List<Integer> scratch_codes = factory.getRecoveryCodes();
                                                    user.send(messages.gAuthScratchCodes(scratch_codes));

                                                    ScratchCodes codes = new ScratchCodes(user.getManager().getUUID());
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
                                if (user.hasPermission(PluginPermissions.force_2fa())) {
                                    user.send(messages.prefix() + messages.gauthLocked());
                                } else {
                                    if (session.isCaptchaLogged() && session.isLogged() && session.isTempLogged()) {
                                        if (args.length >= 3) {
                                            if (manager.has2FA()) {
                                                String password = args[1];

                                                StringBuilder codeBuilder = new StringBuilder();
                                                for (int i = 2; i < args.length; i++)
                                                    codeBuilder.append(args[i]);

                                                try {
                                                    int code = Integer.parseInt(codeBuilder.toString());

                                                    CryptoFactory util = CryptoFactory.getBuilder().withPassword(password).withToken(manager.getPassword()).build();
                                                    GoogleAuthFactory factory = user.getTokenFactory();

                                                    if (util.validate(Validation.ALL) && factory.validate(manager.getGAuth(), code)) {
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
                                            ScratchCodes codes = new ScratchCodes(user.getManager().getUUID());

                                            if (factory.validate(manager.getGAuth(), code) || codes.validate(code)) {
                                                user.setTempSpectator(false);

                                                session.set2FALogged(true);
                                                session.setPinLogged(true);

                                                UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2,
                                                        UserAuthenticateEvent.Result.SUCCESS,
                                                        user.getModule(),
                                                        messages.gAuthCorrect(),
                                                        null);
                                                ModulePlugin.callEvent(event);

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

                                                TransientMap.apply(player);
                                            } else {
                                                UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2,
                                                        UserAuthenticateEvent.Result.FAILED,
                                                        user.getModule(),
                                                        messages.gAuthIncorrect(),
                                                        null);
                                                ModulePlugin.callEvent(event);

                                                user.send(messages.prefix() + event.getAuthMessage());
                                            }
                                        } catch (Throwable ex) {
                                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.FA_2,
                                                    UserAuthenticateEvent.Result.ERROR,
                                                    user.getModule(),
                                                    messages.gAuthIncorrect(),
                                                    null);
                                            ModulePlugin.callEvent(event);

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
            console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }
        return false;
    }
}
