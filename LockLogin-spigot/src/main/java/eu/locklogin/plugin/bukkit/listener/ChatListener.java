package eu.locklogin.plugin.bukkit.listener;

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
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import static eu.locklogin.plugin.bukkit.LockLogin.properties;

public final class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        User user = new User(player);
        ClientSession session = user.getSession();

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PluginMessages messages = CurrentPlatform.getMessages();

        if (session.isValid()) {
            if (!session.isLogged() || !session.isTempLogged()) {
                if (user.getManager().has2FA() && session.isLogged()) {
                    e.setCancelled(true);
                    user.send(messages.prefix() + messages.gAuthenticate());
                } else {
                    if (!AllowedCommand.isAllowed(getCommand(e.getMessage()))) {
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

        if (!config.isBungeeCord()) {
            if (ModulePlugin.parseCommand(e.getMessage())) {
                e.setCancelled(true);
                ModulePlugin.fireCommand(user.getModule(), e.getMessage(), e);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        User user = new User(player);
        ClientSession session = user.getSession();

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PluginMessages messages = CurrentPlatform.getMessages();

        if (session.isValid()) {
            if (!session.isLogged()) {
                String command = getCommand(e.getMessage());
                e.setCancelled(!command.equals("register") && !command.equals("login") && !command.equals("log") && !command.equals("reg") && !AllowedCommand.isAllowed(command));

                return;
            } else {
                if (!session.isTempLogged() && user.getManager().has2FA()) {
                    String command = getCommand(e.getMessage());

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

        if (!config.isBungeeCord()) {
            if (ModulePlugin.parseCommand(e.getMessage())) {
                e.setCancelled(true);
                ModulePlugin.fireCommand(user.getModule(), e.getMessage(), e);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onConsoleCommand(ServerCommandEvent e) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        if (!config.isBungeeCord()) {
            if (ModulePlugin.parseCommand(e.getCommand())) {
                PluginModule module = ModulePlugin.getCommandOwner(e.getCommand());

                if (module != null) {
                    e.setCancelled(true);
                    ModulePlugin.fireCommand(module.getConsole(), e.getCommand(), e);
                }
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
