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

import eu.locklogin.plugin.bungee.plugin.sender.DataSender;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.module.plugin.api.event.user.UserQuitEvent;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleManager;
import eu.locklogin.plugin.bungee.util.player.User;
import eu.locklogin.api.common.security.client.IpData;
import eu.locklogin.api.common.session.SessionKeeper;
import eu.locklogin.api.common.utils.DataType;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.net.InetSocketAddress;

import static eu.locklogin.plugin.bungee.LockLogin.fromPlayer;
import static eu.locklogin.plugin.bungee.LockLogin.getSocketIp;

public final class QuitListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onQuit(PlayerDisconnectEvent e) {
        ProxiedPlayer player = e.getPlayer();

        if (!player.isConnected()) {
            InetSocketAddress ip = getSocketIp(player.getSocketAddress());
            User user = new User(player);

            SessionKeeper keeper = new SessionKeeper(fromPlayer(player));
            keeper.store();

            if (ip != null) {
                IpData data = new IpData(ip.getAddress());
                data.delClone();

                ClientSession session = user.getSession();
                session.invalidate();
                session.setLogged(false);
                session.setPinLogged(false);
                session.set2FALogged(false);

                UserQuitEvent event = new UserQuitEvent(fromPlayer(e.getPlayer()), e);
                JavaModuleManager.callEvent(event);

                DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onKick(ServerKickEvent e) {
        ProxiedPlayer player = e.getPlayer();

        if (!player.isConnected()) {
            InetSocketAddress ip = getSocketIp(player.getSocketAddress());
            User user = new User(player);

            SessionKeeper keeper = new SessionKeeper(fromPlayer(player));
            keeper.store();

            if (ip != null) {
                IpData data = new IpData(ip.getAddress());
                data.delClone();

                ClientSession session = user.getSession();
                session.invalidate();
                session.setLogged(false);
                session.setPinLogged(false);
                session.set2FALogged(false);

                UserQuitEvent event = new UserQuitEvent(fromPlayer(e.getPlayer()), e);
                JavaModuleManager.callEvent(event);

                DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());
            }
        }
    }
}
