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
import eu.locklogin.api.common.session.SessionKeeper;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.UserQuitEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.plugin.bungee.data.MessagePool;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.files.data.Spawn;
import eu.locklogin.plugin.bukkit.util.player.ClientVisor;
import eu.locklogin.plugin.bukkit.util.player.User;
import eu.locklogin.plugin.bukkit.util.player.UserDatabase;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.worker.AsyncLateScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.tryAsync;

public final class QuitListener implements Listener {

    private final static Set<UUID> kicked = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (!kicked.contains(player.getUniqueId())) {
            LateScheduler<Event> result = new AsyncLateScheduler<>();

            tryAsync(TaskTarget.EVENT, () -> {
                PluginConfiguration config = CurrentPlatform.getConfiguration();
                User user = new User(player);

                if (!config.isBungeeCord()) {
                    if (user.getChecker().isUnderCheck()) {
                        user.getChecker().cancelCheck();
                    }

                    if (user.isLockLoginUser()) {
                        if (!config.isBungeeCord()) {
                            SessionKeeper keeper = new SessionKeeper(user.getModule());
                            keeper.store();
                        }

                        //Last location will always be saved since if the server
                        //owner wants to enable it, it would be good to see
                        //the player last location has been stored to avoid
                        //location problems
                        if (Spawn.isAway(player)) {
                            LastLocation last_loc = new LastLocation(player);
                            last_loc.save();
                        }

                        ClientSession session = user.getSession();
                        session.invalidate();
                        session.setLogged(false);
                        session.setPinLogged(false);
                        session.set2FALogged(false);

                        user.removeLockLoginUser();
                    }

                    user.removeSessionCheck();
                    user.setTempSpectator(false);

                    ClientVisor visor = new ClientVisor(player);
                    visor.forceView();

                    UserDatabase.removeUser(player);
                    Event event = new UserQuitEvent(user.getModule(), e);
                    result.complete(event);
                } else {
                    //This is exactly the same as done with "DataType.QUIT" in BungeeListener but in a "safe" mode
                    if (user.isLockLoginUser()) {
                        ClientSession session = user.getSession();

                        //Last location will be always saved since if the server
                        //owner wants to enable it, it would be good to see
                        //the player last location has been stored to avoid
                        //location problems
                        LastLocation last_loc = new LastLocation(player);
                        last_loc.save();

                        session.invalidate();
                        session.setLogged(false);
                        session.setPinLogged(false);
                        session.set2FALogged(false);

                        user.removeLockLoginUser();
                    }

                    user.setTempSpectator(false);
                }
            });

            result.whenComplete(ModulePlugin::callEvent);
        } else {
            kicked.remove(player.getUniqueId());
        }

        MessagePool.delPlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onKick(PlayerKickEvent e) {
        Player player = e.getPlayer();
        kicked.add(player.getUniqueId());

        LateScheduler<Event> result = new AsyncLateScheduler<>();
        tryAsync(TaskTarget.EVENT, () -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            User user = new User(player);

            if (!config.isBungeeCord()) {
                if (user.getChecker().isUnderCheck()) {
                    user.getChecker().cancelCheck();
                }

                if (user.isLockLoginUser()) {
                    if (!config.isBungeeCord()) {
                        SessionKeeper keeper = new SessionKeeper(user.getModule());
                        keeper.store();
                    }

                    //Last location will always be saved since if the server
                    //owner wants to enable it, it would be good to see
                    //the player last location has been stored to avoid
                    //location problems
                    if (Spawn.isAway(player)) {
                        LastLocation last_loc = new LastLocation(player);
                        last_loc.save();
                    }

                    ClientSession session = user.getSession();
                    session.invalidate();
                    session.setLogged(false);
                    session.setPinLogged(false);
                    session.set2FALogged(false);

                    user.removeLockLoginUser();
                }

                user.removeSessionCheck();
                user.setTempSpectator(false);

                ClientVisor visor = new ClientVisor(player);
                visor.forceView();

                UserDatabase.removeUser(player);
                Event event = new UserQuitEvent(user.getModule(), e);
                result.complete(event);
            } else {
                if (user.isLockLoginUser()) {
                    ClientSession session = user.getSession();

                    //Last location will be always saved since if the server
                    //owner wants to enable it, it would be good to see
                    //the player last location has been stored to avoid
                    //location problems
                    LastLocation last_loc = new LastLocation(player);
                    last_loc.save();

                    session.invalidate();
                    session.setLogged(false);
                    session.setPinLogged(false);
                    session.set2FALogged(false);

                    user.removeLockLoginUser();
                }

                user.setTempSpectator(false);
            }
        });

        result.whenComplete(ModulePlugin::callEvent);
    }
}
