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
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.Player;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.client.ClientData;
import eu.locklogin.api.common.session.SessionKeeper;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.module.plugin.api.event.user.UserQuitEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.player.User;

import java.net.InetSocketAddress;

import static eu.locklogin.plugin.velocity.LockLogin.fromPlayer;

public final class QuitListener {

    @Subscribe(order = PostOrder.LAST)
    public final void onQuit(DisconnectEvent e) {
        Player player = e.getPlayer();

        if (!player.isActive()) {
            InetSocketAddress ip = player.getRemoteAddress();
            User user = new User(player);
            if (user.getChecker().isUnderCheck()) {
                user.getChecker().cancelCheck();
            }

            SessionKeeper keeper = new SessionKeeper(fromPlayer(player));
            keeper.store();

            if (ip != null) {
                ClientData data = new ClientData(ip.getAddress());
                data.removeClient(ClientData.getNameByID(player.getUniqueId()));

                ClientSession session = user.getSession();
                session.invalidate();
                session.setLogged(false);
                session.setPinLogged(false);
                session.set2FALogged(false);

                UserQuitEvent event = new UserQuitEvent(fromPlayer(e.getPlayer()), e);
                ModulePlugin.callEvent(event);

                DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());
            }
            user.removeSessionCheck();
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public final void onKick(KickedFromServerEvent e) {
        Player player = e.getPlayer();

        if (!player.isActive()) {
            InetSocketAddress ip = player.getRemoteAddress();
            User user = new User(player);
            if (user.getChecker().isUnderCheck()) {
                user.getChecker().cancelCheck();
            }

            SessionKeeper keeper = new SessionKeeper(fromPlayer(player));
            keeper.store();

            if (ip != null) {
                ClientData data = new ClientData(ip.getAddress());
                data.removeClient(ClientData.getNameByID(player.getUniqueId()));

                ClientSession session = user.getSession();
                session.invalidate();
                session.setLogged(false);
                session.setPinLogged(false);
                session.set2FALogged(false);

                UserQuitEvent event = new UserQuitEvent(fromPlayer(e.getPlayer()), e);
                ModulePlugin.callEvent(event);

                DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());
            }
            user.removeSessionCheck();
        }
    }
}
