package ml.karmaconfigs.locklogin.plugin.bukkit.listener;

import ml.karmaconfigs.locklogin.api.LockLoginListener;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.event.user.UserQuitEvent;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.LastLocation;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.security.client.IpData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.InetSocketAddress;

public final class QuitListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onQuit_APICall(PlayerQuitEvent e) {
        UserQuitEvent event = new UserQuitEvent(e.getPlayer(), e);
        LockLoginListener.callEvent(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onKick_APICall(PlayerKickEvent e) {
        UserQuitEvent event = new UserQuitEvent(e.getPlayer(), e);
        LockLoginListener.callEvent(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        InetSocketAddress ip = player.getAddress();
        User user = new User(player);

        if (user.isLockLoginUser()) {
            if (ip != null) {
                IpData data = new IpData(ip.getAddress());
                data.delClone();
            }

            //Last location will be always saved since if the server
            //owner wants to enable it, it would be good to see
            //the player last location has been stored to avoid
            //location problems
            LastLocation last_loc = new LastLocation(player);
            last_loc.save();

            ClientSession session = user.getSession();
            session.invalidate();

            user.removeLockLoginUser();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onKick(PlayerKickEvent e) {
        Player player = e.getPlayer();
        InetSocketAddress ip = player.getAddress();
        User user = new User(player);

        if (user.isLockLoginUser()) {
            if (ip != null) {
                IpData data = new IpData(ip.getAddress());
                data.delClone();
            }

            //Last location will be always saved since if the server
            //owner wants to enable it, it would be good to see
            //the player last location has been stored to avoid
            //location problems
            LastLocation last_loc = new LastLocation(player);
            last_loc.save();

            ClientSession session = user.getSession();
            session.invalidate();

            user.removeLockLoginUser();
        }
    }
}
