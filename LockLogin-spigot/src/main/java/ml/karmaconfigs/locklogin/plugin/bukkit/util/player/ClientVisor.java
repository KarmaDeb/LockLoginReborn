package ml.karmaconfigs.locklogin.plugin.bukkit.util.player;

import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

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
