package eu.locklogin.plugin.velocity.listener;

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

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.AllowedCommand;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.util.player.User;

import static eu.locklogin.plugin.velocity.LockLogin.properties;
import static eu.locklogin.plugin.velocity.LockLogin.fromPlayer;

public final class ChatListener {

    @Subscribe(order = PostOrder.FIRST)
    public final void onChat(PlayerChatEvent e) {
        Player player = e.getPlayer();
        User user = new User(player);
        ClientSession session = user.getSession();

        PluginMessages messages = CurrentPlatform.getMessages();

        if (session.isValid()) {
            if (!session.isLogged() || !session.isTempLogged()) {
                if (user.getManager().has2FA() && session.isLogged()) {
                    e.setResult(PlayerChatEvent.ChatResult.denied());
                    user.send(messages.prefix() + messages.gAuthenticate());
                } else {
                    if (!AllowedCommand.isAllowed(getCommand(e.getMessage()))) {
                        e.setResult(PlayerChatEvent.ChatResult.denied());
                        return;
                    }
                }
            }
        } else {
            //Instantly cancel the event if the player session is not validated by LockLogin
            user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            e.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }

        if (ModulePlugin.parseCommand(e.getMessage())) {
            e.setResult(PlayerChatEvent.ChatResult.denied());
            ModulePlugin.fireCommand(fromPlayer(player), e.getMessage(), e);
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onCommand(CommandExecuteEvent e) {
        if (e.getCommandSource() instanceof Player) {
            Player player = (Player) e.getCommandSource();

            User user = new User(player);
            ClientSession session = user.getSession();

            PluginMessages messages = CurrentPlatform.getMessages();

            if (session.isValid()) {
                if (!session.isLogged()) {
                    String command = getCommand(e.getCommand());
                    if (!command.equals("register") && !command.equals("login") && !AllowedCommand.isAllowed(command)) {
                        e.setResult(CommandExecuteEvent.CommandResult.denied());
                    }

                    return;
                } else {
                    if (!session.isTempLogged() && user.getManager().has2FA()) {
                        String command = getCommand(e.getCommand());

                        if (!command.equals("2fa")) {
                            e.setResult(CommandExecuteEvent.CommandResult.denied());
                            user.send(messages.prefix() + messages.gAuthenticate());
                        }
                    }
                }
            } else {
                //Instantly cancel the event if the player session is not validated by LockLogin
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
                e.setResult(CommandExecuteEvent.CommandResult.denied());
                return;
            }

            if (ModulePlugin.parseCommand(e.getCommand())) {
                e.setResult(CommandExecuteEvent.CommandResult.denied());
                ModulePlugin.fireCommand(fromPlayer(player), e.getCommand(), e);
            }
        }
    }

    /**
     * Get the main command from the cmd
     * even if it has :
     *
     * @param cmd the cmd
     * @return a command ignoring ":" prefix
     */
    private String getCommand(String cmd) {
        if (cmd.contains(":")) {
            try {
                String[] cmdData = cmd.split(":");

                if (cmdData[0] != null && !cmdData[0].isEmpty()) {
                    if (cmdData[1] != null && !cmdData[1].isEmpty()) {
                        return cmdData[1];
                    }
                }
            } catch (Throwable ignored) {
            }
            return cmd.split(" ")[0].replace("/", "");
        } else {
            if (cmd.contains(" ")) {
                return cmd.split(" ")[0].replace("/", "");
            } else {
                return cmd.replace("/", "");
            }
        }
    }
}
