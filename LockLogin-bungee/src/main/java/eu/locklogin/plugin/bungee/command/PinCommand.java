package eu.locklogin.plugin.bungee.command;

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
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.command.util.SystemCommand;
import eu.locklogin.plugin.bungee.plugin.Manager;
import eu.locklogin.plugin.bungee.util.player.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.List;
import java.util.UUID;

import static eu.locklogin.plugin.bungee.LockLogin.console;
import static eu.locklogin.plugin.bungee.LockLogin.properties;

@SystemCommand(command = "pin")
@SuppressWarnings("unused")
public class PinCommand extends Command {

    /**
     * Construct a new command with no permissions or aliases.
     *
     * @param name    the name of this command
     * @param aliases the command aliases
     */
    public PinCommand(final String name, final List<String> aliases) {
        super(name, "", aliases.toArray(new String[0]));
    }

    /**
     * Execute this command with the specified sender and arguments.
     *
     * @param sender the executor of this command
     * @param tmpArgs   arguments used to invoke this command
     */
    @Override
    public void execute(CommandSender sender, String[] tmpArgs) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PluginMessages messages = CurrentPlatform.getMessages();

        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
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

                    return;
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

                                            try {
                                                Integer.parseInt(pin);
                                                if (pin.length() == 4) {
                                                    manager.setPin(pin);
                                                    user.send(messages.prefix() + messages.pinSet());

                                                    session.setPinLogged(false);

                                                    Manager.sendFunction.apply(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT, player)
                                                            .addProperty("pin", true).getInstance(), BungeeSender.serverFromPlayer(player));

                                                    CurrentPlatform.requestDataContainerUpdate();
                                                } else {
                                                    user.send(messages.prefix() + messages.pinLength());
                                                }
                                            } catch (NumberFormatException ex) {
                                                user.send(messages.prefix() + messages.pinLength());
                                            }
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
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }
    }
}
