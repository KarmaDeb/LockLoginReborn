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
import eu.locklogin.api.common.session.persistence.SessionKeeper;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.module.plugin.api.event.user.UserQuitEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.plugin.Manager;
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

    private final Set<UUID> kicked = Collections.newSetFromMap(new ConcurrentHashMap<>());
    static final Set<UUID> tmp_clients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final JoinListener instance;

    public QuitListener(final JoinListener l) {
        instance = l;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerDisconnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        if (!kicked.contains(player.getUniqueId())) {
            if (!player.isConnected()) {
                tmp_clients.remove(player.getUniqueId());

                User user = new User(player);
                if (user.getChecker().isUnderCheck()) {
                    user.getChecker().cancelCheck("User left");
                }

                SessionKeeper keeper = new SessionKeeper(user.getModule());
                keeper.store();

                ClientSession session = user.getSession();
                session.invalidate();
                session.setLogged(false);
                session.setPinLogged(false);
                session.set2FALogged(false);

                user.removeSessionCheck();
                UserDatabase.removeUser(player);

                Event event = new UserQuitEvent(user.getModule(), e);
                ModulePlugin.callEvent(event);

                instance.pool.delPlayer(player.getUniqueId());
                instance.switch_pool.delPlayer(player.getUniqueId());
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
            tmp_clients.remove(player.getUniqueId());

            User user = new User(player);
            if (user.getChecker().isUnderCheck()) {
                user.getChecker().cancelCheck("User kicked");
            }

            SessionKeeper keeper = new SessionKeeper(user.getModule());
            keeper.store();

            ClientSession session = user.getSession();
            session.invalidate();
            session.setLogged(false);
            session.setPinLogged(false);
            session.set2FALogged(false);

            Manager.sendFunction.apply(DataMessage.newInstance(DataType.QUIT, Channel.ACCOUNT, player)
                    .getInstance(), BungeeSender.serverFromPlayer(player));

            user.removeSessionCheck();
            UserDatabase.removeUser(player);

            Event event = new UserQuitEvent(user.getModule(), e);
            ModulePlugin.callEvent(event);

            instance.pool.delPlayer(player.getUniqueId());
            instance.switch_pool.delPlayer(player.getUniqueId());
        }
    }
}
