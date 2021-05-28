package ml.karmaconfigs.locklogin.plugin.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.UserQuitEvent;
import ml.karmaconfigs.locklogin.plugin.common.security.client.IpData;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionKeeper;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;

import java.net.InetSocketAddress;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.fromPlayer;

public final class QuitListener {

    @Subscribe(order = PostOrder.LAST)
    public final void onQuit_APICall(DisconnectEvent e) {
        UserQuitEvent event = new UserQuitEvent(fromPlayer(e.getPlayer()), e);
        JavaModuleManager.callEvent(event);
    }

    @Subscribe(order = PostOrder.LAST)
    public final void onKick_APICall(KickedFromServerEvent e) {
        UserQuitEvent event = new UserQuitEvent(fromPlayer(e.getPlayer()), e);
        JavaModuleManager.callEvent(event);
    }

    @Subscribe(order = PostOrder.FIRST)
    public final void onQuit(DisconnectEvent e) {
        Player player = e.getPlayer();

        if (!player.isActive()) {
            InetSocketAddress ip = player.getRemoteAddress();
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

                DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());
            }
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public final void onKick(KickedFromServerEvent e) {
        Player player = e.getPlayer();

        if (!player.isActive()) {
            InetSocketAddress ip = player.getRemoteAddress();
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

                DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());
            }
        }
    }
}
