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
import eu.locklogin.api.common.security.client.CommandProxy;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.LockLogin;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.util.inventory.PinInventory;
import eu.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static eu.locklogin.plugin.bukkit.LockLogin.console;

@SystemCommand(command = "pin")
public class PinCommand implements CommandExecutor {

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

                if (config.enablePin()) {
                    if (session.isCaptchaLogged() && session.isLogged() && session.isTempLogged()) {
                        if (args.length == 0) {
                            user.send(messages.prefix() + messages.pinUsages());
                        } else {
                            switch (args[0].toLowerCase()) {
                                case "setup":
                                    if (args.length == 2) {
                                        if (!manager.hasPin()) {
                                            String pin = args[1];

                                            manager.setPin(pin);
                                            user.send(messages.prefix() + messages.pinSet());

                                            session.setPinLogged(false);

                                            PinInventory inventory = new PinInventory(player);
                                            inventory.open();
                                        } else {
                                            user.send(messages.prefix() + messages.alreadyPin());
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.setPin());
                                    }
                                    break;
                                case "remove":
                                    if (args.length == 2) {
                                        if (!manager.hasPin()) {
                                            user.send(messages.prefix() + messages.noPin());
                                        } else {
                                            String current = args[1];

                                            CryptoFactory util = CryptoFactory.getBuilder().withPassword(current).withToken(manager.getPin()).build();
                                            if (util.validate(Validation.ALL)) {
                                                manager.setUnsafePin("");
                                                user.send(messages.prefix() + messages.pinReseted());
                                            } else {
                                                user.send(messages.prefix() + messages.incorrectPin());
                                            }
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.resetPin());
                                    }
                                    break;
                                case "change":
                                    if (args.length == 3) {
                                        if (!manager.hasPin()) {
                                            user.send(messages.prefix() + messages.noPin());
                                        } else {
                                            String current = args[1];
                                            String newPin = args[2];

                                            CryptoFactory util = CryptoFactory.getBuilder().withPassword(current).withToken(manager.getPin()).build();
                                            if (util.validate(Validation.ALL)) {
                                                manager.setPin(newPin);
                                                user.send(messages.prefix() + messages.pinChanged());
                                            } else {
                                                user.send(messages.prefix() + messages.incorrectPin());
                                            }
                                        }
                                    } else {
                                        user.send(messages.prefix() + messages.changePin());
                                    }
                                    break;
                                default:
                                    user.send(messages.prefix() + messages.pinUsages());
                                    break;
                            }
                        }
                    }
                } else {
                    user.send(messages.prefix() + messages.pinDisabled());
                }
            } else {
                user.send(messages.prefix() + LockLogin.properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            console.send(messages.prefix() + LockLogin.properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }

        return false;
    }
}
