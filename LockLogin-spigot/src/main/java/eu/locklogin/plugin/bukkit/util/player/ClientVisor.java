package eu.locklogin.plugin.bukkit.util.player;

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
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

/**
 * Client visor class.
 *
 * NOTE: 10/05/22 - 01:28
 * HOPE THIS UPDATE WORKS, I DON'T KNOW HOW MANY TIMES
 * I'VE UPDATED THIS CODE AND STILL GIVING BUGS. PLEASE
 * WORK, I HAVE OTHER THINGS TO DO WITH THIS PLUGIN...
 */
@SuppressWarnings("deprecation")
public final class ClientVisor {

    private final Player player;
    private final Map<UUID, Set<UUID>> locklogin_vanished = new HashMap<>();

    /**
     * Initialize the client visor
     *
     * @param cl the client
     */
    public ClientVisor(final Player cl) {
        player = cl;
    }

    /**
     * Toggle the client visibility. Everywhere
     */
    public void toggleView() {
        //This is the easiest part. Just make the player can't see anyone. And anyone can see the player
        UUID id = player.getUniqueId();

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            //We don't want to do it to himself ( even though it wouldn't work if we did )
            if (!online.getUniqueId().equals(id)) {
                if (player.canSee(online)) {
                    hide(player, online);
                }
                if (online.canSee(player)) {
                    hide(online, player);
                }
            }
        }

        //Now is where problems start. We must create a timer that will run until the player
        //is logged so every new player can't see the non logged player
        SimpleScheduler scheduler = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true);
        scheduler.restartAction(() -> {
            User user = new User(player);
            ClientSession session = user.getSession();

            if (session.isLogged() && session.isTempLogged()) {
                //KarmaAPI sync worker doesn't work actually on the main thread. It will try to run the queried function into the main thread
                //but that doesn't mean it will. So we must use bukkit's sync worker
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (Player online : plugin.getServer().getOnlinePlayers()) {
                        User tmp = new User(online);
                        ClientSession tmpSession = tmp.getSession();

                        if (tmpSession.isLogged() && tmpSession.isTempLogged()) {
                            show(online, player);
                            show(player, online);
                        }
                    }
                });

                scheduler.cancel();
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (Player online : plugin.getServer().getOnlinePlayers()) {
                        if (player.canSee(online)) {
                            hide(player, online);
                        }
                        if (online.canSee(player)) {
                            hide(online, player);
                        }
                    }
                });
            }
        });

        scheduler.start();
    }

    /**
     * In case the client disconnects or anything. We want everyone to make see him
     * again. Or if someone disconnects, we want he be able to see the client the
     * next time he joins the server.
     */
    public void forceView() {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            show(online, player);
            show(player, online);
        }
    }

    /**
     * Hide the target player from the origin player
     *
     * @param origin the origin player
     * @param target the target player
     */
    private void hide(final Player origin, final Player target) {
        if (!canSee(origin, target)) {
            try {
                origin.hidePlayer(target);
            } catch (Throwable ex) {
                origin.hidePlayer(plugin, target);
            }

            Set<UUID> vanished = locklogin_vanished.getOrDefault(origin.getUniqueId(), Collections.newSetFromMap(new ConcurrentHashMap<>()));
            vanished.add(target.getUniqueId());

            locklogin_vanished.put(origin.getUniqueId(), vanished);
        }
    }

    /**
     * Show the target player to the origin player
     *
     * @param origin the origin player
     * @param target the target player
     */
    private void show(final Player origin, final Player target) {
        if (canSee(origin, target)) {
            try {
                origin.showPlayer(target);
            } catch (Throwable ex) {
                //Thanks spigot :)
                origin.showPlayer(plugin, target);
            }

            Set<UUID> vanished = locklogin_vanished.getOrDefault(origin.getUniqueId(), Collections.newSetFromMap(new ConcurrentHashMap<>()));
            vanished.remove(target.getUniqueId());

            locklogin_vanished.put(origin.getUniqueId(), vanished);
        }
    }

    /**
     * Get if the player can see the target player
     *
     * @param origin the player
     * @param target the target player
     * @return if the player can see the target
     */
    private boolean canSee(final Player origin, final Player target) {
        Set<UUID> vanished = locklogin_vanished.getOrDefault(origin.getUniqueId(), Collections.newSetFromMap(new ConcurrentHashMap<>()));
        return !vanished.contains(target.getUniqueId());
    }
}
