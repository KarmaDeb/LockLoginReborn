package ml.karmaconfigs.locklogin.plugin.bungee.listener;

import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.modules.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.event.user.UserQuitEvent;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataType;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.client.IpData;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.net.InetSocketAddress;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.fromPlayer;
import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.getSocketIp;

public final class QuitListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onQuit_APICall(ServerDisconnectEvent e) {
        UserQuitEvent event = new UserQuitEvent(fromPlayer(e.getPlayer()), e);
        JavaModuleManager.callEvent(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onKick_APICall(ServerKickEvent e) {
        UserQuitEvent event = new UserQuitEvent(fromPlayer(e.getPlayer()), e);
        JavaModuleManager.callEvent(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onQuit(PlayerDisconnectEvent e) {
        ProxiedPlayer player = e.getPlayer();

        if (!player.isConnected()) {
            InetSocketAddress ip = getSocketIp(player.getSocketAddress());
            User user = new User(player);

            if (ip != null) {
                IpData data = new IpData(ip.getAddress());
                data.delClone();

                ClientSession session = user.getSession();
                session.invalidate();
                session.setLogged(false);
                session.setPinLogged(false);
                session.set2FALogged(false);

                DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER).build());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onKick(ServerKickEvent e) {
        ProxiedPlayer player = e.getPlayer();

        if (!player.isConnected()) {
            InetSocketAddress ip = getSocketIp(player.getSocketAddress());
            User user = new User(player);

            if (ip != null) {
                IpData data = new IpData(ip.getAddress());
                data.delClone();

                ClientSession session = user.getSession();
                session.invalidate();
                session.setLogged(false);
                session.setPinLogged(false);
                session.set2FALogged(false);

                DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER).build());
            }
        }
    }
}
