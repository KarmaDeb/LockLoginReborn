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

import eu.locklogin.api.common.security.client.ClientData;
import eu.locklogin.plugin.bukkit.LockLogin;
import eu.locklogin.plugin.bukkit.util.files.Config;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.player.ClientVisor;
import eu.locklogin.plugin.bukkit.util.player.User;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.module.plugin.api.event.user.UserQuitEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.common.session.SessionKeeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.InetSocketAddress;

public final class QuitListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        InetSocketAddress ip = player.getAddress();
        User user = new User(player);

        if (user.isLockLoginUser()) {
            if (ip != null) {
                ClientData client = new ClientData(ip.getAddress());
                client.removeClient(ClientData.getNameByID(player.getUniqueId()));
            }

            Config config = new Config();
            if (!config.isBungeeCord()) {
                SessionKeeper keeper = new SessionKeeper(LockLogin.fromPlayer(player));
                keeper.store();
            }

            //Last location will be always saved since if the server
            //owner wants to enable it, it would be good to see
            //the player last location has been stored to avoid
            //location problems
            LastLocation last_loc = new LastLocation(player);
            last_loc.save();

            ClientSession session = user.getSession();
            session.invalidate();
            session.setLogged(false);
            session.setPinLogged(false);
            session.set2FALogged(false);

            user.removeLockLoginUser();
        }

        user.setTempSpectator(false);

        ClientVisor visor = new ClientVisor(player);
        visor.unVanish();
        visor.checkVanish();

        UserQuitEvent event = new UserQuitEvent(LockLogin.fromPlayer(e.getPlayer()), e);
        ModulePlugin.callEvent(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onKick(PlayerKickEvent e) {
        Player player = e.getPlayer();
        InetSocketAddress ip = player.getAddress();
        User user = new User(player);

        if (user.isLockLoginUser()) {
            if (ip != null) {
                ClientData client = new ClientData(ip.getAddress());
                client.removeClient(ClientData.getNameByID(player.getUniqueId()));
            }

            Config config = new Config();
            if (!config.isBungeeCord()) {
                SessionKeeper keeper = new SessionKeeper(LockLogin.fromPlayer(player));
                keeper.store();
            }

            //Last location will be always save since if the server
            //owner wants to enable it, it would be good to see
            //the player last location has been stored to avoid
            //location problems
            LastLocation last_loc = new LastLocation(player);
            last_loc.save();

            ClientSession session = user.getSession();
            session.invalidate();
            session.setLogged(false);
            session.setPinLogged(false);
            session.set2FALogged(false);

            user.removeLockLoginUser();
        }

        user.setTempSpectator(false);

        ClientVisor visor = new ClientVisor(player);
        visor.unVanish();
        visor.checkVanish();

        UserQuitEvent event = new UserQuitEvent(LockLogin.fromPlayer(e.getPlayer()), e);
        ModulePlugin.callEvent(event);
    }
}
