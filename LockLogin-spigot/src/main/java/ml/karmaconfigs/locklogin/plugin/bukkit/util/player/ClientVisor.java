package ml.karmaconfigs.locklogin.plugin.bukkit.util.player;

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

import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.plugin;
import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.tryAsync;

@SuppressWarnings("deprecation")
public final class ClientVisor {

    private final Player player;

    private final static Set<UUID> vanished = new LinkedHashSet<>();

    /**
     * Initialize the client visor
     *
     * @param _player the player
     */
    public ClientVisor(final Player _player) {
        player = _player;
    }

    /**
     * Vanish the specified player
     */
    public final void vanish() {
        vanished.add(player.getUniqueId());

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            player.hidePlayer(online);
            online.hidePlayer(player);
        }
    }

    /**
     * Show the player again
     * to other players
     */
    public final void unVanish() {
        vanished.remove(player.getUniqueId());

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            player.showPlayer(online);
            online.showPlayer(player);
        }
    }

    /**
     * Check for all vanished players
     */
    public final void checkVanish() {
        tryAsync(() -> {
            Player last = null;

            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!vanished.contains(online.getUniqueId())) {
                    for (UUID id : vanished) {
                        Player vanished = plugin.getServer().getPlayer(id);
                        if (vanished != null && vanished.isOnline()) {
                            if (online.canSee(vanished) || vanished.canSee(online)) {
                                online.hidePlayer(vanished);
                                vanished.hidePlayer(online);
                            }
                        }
                    }
                }

                if (last != null) {
                    if (!vanished.contains(last.getUniqueId()) && !vanished.contains(online.getUniqueId())) {
                        if (!online.canSee(last) || !last.canSee(online)) {
                            online.showPlayer(last);
                            last.showPlayer(online);
                        }
                    }
                }

                last = online;
            }
        });
    }
}
