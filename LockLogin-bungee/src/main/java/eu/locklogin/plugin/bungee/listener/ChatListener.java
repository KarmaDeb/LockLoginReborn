package eu.locklogin.plugin.bungee.listener;

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

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.AllowedCommand;
import eu.locklogin.api.common.security.client.CommandProxy;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.util.player.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Arrays;
import java.util.UUID;

import static eu.locklogin.plugin.bungee.LockLogin.properties;

public final class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(ChatEvent e) {
        if (e.getMessage().startsWith("/")) {
            parseCommand(e);
        } else {
            ProxiedPlayer player = (ProxiedPlayer) e.getSender();
            User user = new User(player);
            ClientSession session = user.getSession();

            PluginMessages messages = CurrentPlatform.getMessages();

            if (session.isValid()) {
                if (!session.isLogged() || !session.isTempLogged()) {
                    if (user.getManager().has2FA() && session.isLogged()) {
                        e.setCancelled(true);
                        user.send(messages.prefix() + messages.gAuthenticate());
                    } else {
                        if (AllowedCommand.notAllowed(getCommand(e.getMessage()))) {
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            } else {
                //Instantly cancel the event if the player session is not validated by LockLogin
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
                e.setCancelled(true);
                return;
            }

            if (ModulePlugin.parseCommand(e.getMessage())) {
                e.setCancelled(true);
                ModulePlugin.fireCommand(user.getModule(), e.getMessage(), e);
            }
        }
    }

    /**
     * Parse the player command
     *
     * @param e the chat event
     */
    private void parseCommand(final ChatEvent e) {
        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        User user = new User(player);
        ClientSession session = user.getSession();

        PluginMessages messages = CurrentPlatform.getMessages();

        String command = getCommand(e.getMessage());
        if (session.isValid()) {
            if (CommandProxy.mustMask(e.getMessage())) {
                UUID mask_id = CommandProxy.mask(e.getMessage(), getArguments(e.getMessage()));
                e.setMessage(CommandProxy.getCommand(mask_id) + " " + mask_id);
            }

            if (!session.isLogged()) {
                e.setCancelled(!command.equals("register") && !command.equals("login") && !command.equals("log") && !command.equals("reg") && !command.equals("panic") && AllowedCommand.notAllowed(command));
                return;
            } else {
                if (!session.isTempLogged() && user.getManager().has2FA()) {
                    if (!command.equals("2fa")) {
                        e.setCancelled(true);
                        user.send(messages.prefix() + messages.gAuthenticate());
                    }
                }
            }
        } else {
            //Instantly cancel the event if the player session is not validated by LockLogin
            user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            e.setCancelled(true);
            return;
        }

        if (ModulePlugin.parseCommand(e.getMessage())) {
            e.setCancelled(true);
            ModulePlugin.fireCommand(user.getModule(), e.getMessage(), e);
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

    /**
     * Get the main command from the cmd
     * even if it has :
     *
     * @param cmd the cmd
     * @return a command ignoring ":" prefix
     */
    @SuppressWarnings("unused")
    private String[] getArguments(String cmd) {
        if (cmd.contains(" ")) {
            String[] cmdData = cmd.split(" ");
            return Arrays.copyOfRange(cmdData, 1, cmdData.length);
        }

        return new String[0];
    }
}
