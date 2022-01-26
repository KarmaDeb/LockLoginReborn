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
import eu.locklogin.api.common.session.SessionKeeper;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.module.plugin.api.event.user.UserQuitEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.plugin.bungee.plugin.sender.DataSender;
import eu.locklogin.plugin.bungee.util.player.User;
import eu.locklogin.plugin.bungee.util.player.UserDatabase;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuitListener implements Listener {

    private final static Set<UUID> kicked = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerDisconnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        if (!kicked.contains(player.getUniqueId())) {
            if (!player.isConnected()) {
                User user = new User(player);
                if (user.getChecker().isUnderCheck()) {
                    user.getChecker().cancelCheck();
                }

                SessionKeeper keeper = new SessionKeeper(user.getModule());
                keeper.store();

                ClientSession session = user.getSession();
                session.invalidate();
                session.setLogged(false);
                session.setPinLogged(false);
                session.set2FALogged(false);

                /*
                This was causing errors, as the player was not longer in the server, this was kind stupid to do, there was literally no
                way the message could arrive the target server as the player is not longer on it...

                As of LockLogin 1.13.15, the actions performed by this message are automatically run from spigot when the player leaves

                DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());
                 */

                user.removeSessionCheck();
                UserDatabase.removeUser(player);

                Event event = new UserQuitEvent(user.getModule(), e);
                ModulePlugin.callEvent(event);
            }
        } else {
            kicked.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKick(ServerKickEvent e) {
        ProxiedPlayer player = e.getPlayer();

        if (!player.isConnected()) {
            kicked.add(player.getUniqueId());

            User user = new User(player);
            if (user.getChecker().isUnderCheck()) {
                user.getChecker().cancelCheck();
            }

            SessionKeeper keeper = new SessionKeeper(user.getModule());
            keeper.store();

            ClientSession session = user.getSession();
            session.invalidate();
            session.setLogged(false);
            session.setPinLogged(false);
            session.set2FALogged(false);

            DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());

            user.removeSessionCheck();
            UserDatabase.removeUser(player);

            Event event = new UserQuitEvent(user.getModule(), e);
            ModulePlugin.callEvent(event);
        }
    }
}
