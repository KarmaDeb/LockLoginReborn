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
import eu.locklogin.api.common.security.BruteForce;
import eu.locklogin.api.common.security.client.CommandProxy;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.plugin.ComponentFactory;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.command.util.SystemCommand;
import eu.locklogin.plugin.bungee.plugin.Manager;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

import static eu.locklogin.plugin.bungee.LockLogin.*;

@SystemCommand(command = "panic")
@SuppressWarnings("unused")
public final class PanicCommand extends Command {

    private final static PluginConfiguration config = CurrentPlatform.getConfiguration();
    private final static PluginMessages messages = CurrentPlatform.getMessages();

    /**
     * Construct a new command with no permissions or aliases.
     *
     * @param name the name of this command
     */
    public PanicCommand(final String name, final List<String> aliases) {
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
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            User user = new User(player);

            ClientSession session = user.getSession();
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

                if (!session.isLogged()) {
                    AccountManager manager = user.getManager();
                    if (!manager.exists())
                        if (manager.create()) {
                            logger.scheduleLog(Level.INFO, "Created account of player {0}", StringUtils.stripColor(player.getDisplayName()));
                        } else {
                            logger.scheduleLog(Level.GRAVE, "Couldn't create account of player {0}", StringUtils.stripColor(player.getDisplayName()));

                            user.send(messages.prefix() + properties.getProperty("could_not_create_user", "&5&oWe're sorry, but we couldn't create your account"));
                            return;
                        }

                    if (!manager.isRegistered()) {
                        user.send(messages.prefix() + messages.register());
                    } else {
                        if (args.length == 0) {
                            user.send(messages.prefix() + messages.panicLogin());
                        } else {
                            String panic = args[0];

                            BruteForce protection = null;
                            InetAddress ip = getIp(player.getSocketAddress());
                            if (ip != null)
                                protection = new BruteForce(ip);

                            if (protection == null) {
                                user.send(messages.prefix() + "&cError 52");
                                return;
                            }

                            if (protection.isPanicking(player.getUniqueId())) {
                                String panicHash = manager.getPanic();
                                if (!panicHash.isEmpty()) {
                                    CryptoFactory factory = CryptoFactory.getBuilder()
                                            .withPassword(panic)
                                            .withToken(panicHash).build();

                                    if (factory.validate(Validation.ALL)) {
                                        protection.unPanic(player.getUniqueId());
                                        protection.success();

                                        session.set2FALogged(true);
                                        session.setPinLogged(true);
                                        session.setCaptchaLogged(true);
                                        session.setLogged(true);

                                        String password = TokenGenerator.generateLiteral(32);

                                        user.send(messages.panicRequested());
                                        ComponentFactory cf = new ComponentFactory(StringUtils.toColor("&7Panic token: &e" + password))
                                                .hover(StringUtils.toColor("&bClick to copy"))
                                                .click(ClickEvent.Action.SUGGEST_COMMAND, password);
                                        user.send(cf.get());

                                        manager.setPanic(password);

                                        protection.success();

                                        user.restorePotionEffects();

                                        Manager.sendFunction.apply(DataMessage.newInstance(DataType.SESSION, Channel.ACCOUNT, player)
                                                .getInstance(), BungeeSender.serverFromPlayer(player));

                                        Manager.sendFunction.apply(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT, player)
                                                .addProperty("pin", false).getInstance(), BungeeSender.serverFromPlayer(player));

                                        Manager.sendFunction.apply(DataMessage.newInstance(DataType.GAUTH, Channel.ACCOUNT, player)
                                                .getInstance(), BungeeSender.serverFromPlayer(player));

                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.API,
                                                UserAuthenticateEvent.Result.SUCCESS,
                                                user.getModule(),
                                                messages.logged(),
                                                null);
                                        ModulePlugin.callEvent(event);

                                        user.checkServer(0, true);
                                    } else {
                                        protection.block(config.bruteForceOptions().getBlockTime());
                                        user.kick(messages.ipBlocked(protection.getBlockLeft()));
                                    }
                                } else {
                                    protection.unPanic(player.getUniqueId());
                                    user.send(messages.prefix() + messages.login());
                                }
                            } else {
                                user.send(messages.prefix() + messages.login());
                            }
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
            console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }
    }
}
