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
import eu.locklogin.plugin.velocity.util.player.UserDatabase;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuitListener {

    private final static Set<UUID> kicked = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Subscribe(order = PostOrder.LAST)
    public void onQuit(DisconnectEvent e) {
        Player player = e.getPlayer();
        if (!kicked.contains(player.getUniqueId())) {
            if (!player.isActive()) {
                InetSocketAddress ip = player.getRemoteAddress();
                User user = new User(player);
                if (user.getChecker().isUnderCheck()) {
                    user.getChecker().cancelCheck();
                }

                SessionKeeper keeper = new SessionKeeper(user.getModule());
                keeper.store();

                if (ip != null) {
                    ClientData data = new ClientData(ip.getAddress());
                    data.removeClient(ClientData.getNameByID(player.getUniqueId()));

                    ClientSession session = user.getSession();
                    session.invalidate();
                    session.setLogged(false);
                    session.setPinLogged(false);
                    session.set2FALogged(false);

                    DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());
                }

                user.removeSessionCheck();
                UserDatabase.removeUser(player);

                UserQuitEvent event = new UserQuitEvent(user.getModule(), e);
                ModulePlugin.callEvent(event);
            }
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onKick(KickedFromServerEvent e) {
        Player player = e.getPlayer();
        kicked.add(player.getUniqueId());
        if (!player.isActive()) {
            InetSocketAddress ip = player.getRemoteAddress();
            User user = new User(player);
            if (user.getChecker().isUnderCheck()) {
                user.getChecker().cancelCheck();
            }

            SessionKeeper keeper = new SessionKeeper(user.getModule());
            keeper.store();

            if (ip != null) {
                ClientData data = new ClientData(ip.getAddress());
                data.removeClient(ClientData.getNameByID(player.getUniqueId()));

                ClientSession session = user.getSession();
                session.invalidate();
                session.setLogged(false);
                session.setPinLogged(false);
                session.set2FALogged(false);

                DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());
            }

            user.removeSessionCheck();
            UserDatabase.removeUser(player);

            UserQuitEvent event = new UserQuitEvent(user.getModule(), e);
            ModulePlugin.callEvent(event);
        }
    }
}
