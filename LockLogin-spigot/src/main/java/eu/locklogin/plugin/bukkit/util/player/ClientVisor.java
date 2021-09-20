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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.locklogin.plugin.bukkit.LockLogin.tryAsync;
import static eu.locklogin.plugin.bukkit.LockLogin.trySync;

@SuppressWarnings("deprecation")
public final class ClientVisor {

    private final Player player;

    private final static Map<UUID, Set<UUID>> vanished = new ConcurrentHashMap<>();

    /**
     * Initialize the client visor
     *
     * @param cl the client
     */
    public ClientVisor(final Player cl) {
        player = cl;
    }

    /**
     * Hide the player
     */
    public void hide() {
        Set<UUID> affected = new HashSet<>();
        Set<UUID> tarAffected;

        for (Player connected : Bukkit.getOnlinePlayers()) {
            tarAffected = vanished.getOrDefault(connected.getUniqueId(), new HashSet<>());

            if (connected.canSee(player)) {
                affected.add(connected.getUniqueId());
            }

            if (player.canSee(connected)) {
                tarAffected.add(player.getUniqueId());
                vanished.put(connected.getUniqueId(), tarAffected);
            }
        }

        vanished.put(player.getUniqueId(), affected);

        check();
    }

    /**
     * Show the player
     */
    public void show() {
        Set<UUID> affected = vanished.getOrDefault(player.getUniqueId(), new HashSet<>());

        for (UUID affect : affected) {
            Set<UUID> tarAffected = vanished.getOrDefault(affect, new HashSet<>());

            tarAffected.remove(player.getUniqueId());
            vanished.put(affect, tarAffected);
        }

        check();
    }

    /**
     * Check everyone can see everyone
     */
    private void check() {
        tryAsync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                User user = new User(player);
                ClientSession session = user.getSession();

                Set<UUID> affected = vanished.getOrDefault(player.getUniqueId(), new HashSet<>());

                if (!affected.isEmpty()) {
                    for (UUID sub : affected) {
                        OfflinePlayer offline = Bukkit.getOfflinePlayer(sub);
                        Player connected = offline.getPlayer();
                        if (connected != null) {
                            User target = new User(connected);
                            ClientSession tarSession = target.getSession();

                            if (session.isLogged() && session.isTempLogged()) {
                                if (tarSession.isLogged() && tarSession.isTempLogged()) {
                                    trySync(() -> {
                                        connected.showPlayer(player);
                                        player.showPlayer(connected);
                                    });

                                    affected.remove(sub);
                                }
                            } else {
                                trySync(() -> {
                                    connected.hidePlayer(player);
                                    player.hidePlayer(connected);
                                });
                            }
                        } else {
                            affected.remove(sub);
                        }
                    }

                    vanished.put(player.getUniqueId(), affected);
                }
            }
        });
    }
}
