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
import eu.locklogin.api.common.security.BruteForce;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SystemCommand(command = "panic")
@SuppressWarnings("unused")
public final class PanicCommand implements CommandExecutor {

    private final static PluginConfiguration config = CurrentPlatform.getConfiguration();
    private final static PluginMessages messages = CurrentPlatform.getMessages();

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
            tryAsync(TaskTarget.COMMAND_EXECUTE, () -> {
                Player player = (Player) sender;
                User user = new User(player);

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
                                if (player.getAddress() != null)
                                    protection = new BruteForce(player.getAddress().getAddress());

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
                                            TextComponent component = new TextComponent(StringUtils.toColor("&7Panic token: &eu.c" + password));
                                            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(StringUtils.toColor("&bClick to copy"))));
                                            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, password));
                                            user.send(component);

                                            manager.setPanic(password);
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
            });
        } else {
            console.send(messages.prefix() + properties.getProperty("command_not_available", "&cThis command is not available for console"));
        }

        return false;
    }
}
