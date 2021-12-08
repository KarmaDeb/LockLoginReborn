package eu.locklogin.api.util.platform;

import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * LockLogin module server
 */
public final class ModuleServer {

    private final static Set<ModulePlayer> connected = Collections.newSetFromMap(new ConcurrentHashMap<>());

    ModuleServer() {
    }

    /**
     * Connect a player
     *
     * @param player the player to connect
     */
    void connectPlayer(final ModulePlayer player) {
        connected.removeIf(online -> online.getUUID().equals(player.getUUID()));
        connected.add(player);
    }

    /**
     * Disconnect a player
     *
     * @param player the player
     */
    void disconnectPlayer(final ModulePlayer player) {
        connected.remove(player);
    }

    /**
     * Get the online players
     *
     * @return the online players
     */
    public Collection<ModulePlayer> getOnlinePlayers() {
        Set<ModulePlayer> unmodifiableSafe = new LinkedHashSet<>(connected);
        Stream<ModulePlayer> stream = unmodifiableSafe.stream().sorted(Comparator.comparing(ModulePlayer::getName));

        return Collections.unmodifiableList(stream.collect(Collectors.toList()));
    }

    /**
     * Get a module player by its UUID
     *
     * @param id the module player UUID
     * @return the module player
     */
    @Nullable
    public ModulePlayer getPlayer(final UUID id) {
        Collection<ModulePlayer> players = getOnlinePlayers();
        for (ModulePlayer player : players) {
            if (player.getUUID().equals(id))
                return player;
        }

        return null;
    }

    /**
     * Get a player by its name
     *
     * @param name the player name
     * @return the module player
     */
    public ModulePlayer[] getPlayer(final String name) {
        List<ModulePlayer> matches = new ArrayList<>();
        Collection<ModulePlayer> players = getOnlinePlayers();

        for (ModulePlayer player : players) {
            if (player.getName().equalsIgnoreCase(name)) {
                if (!matches.contains(player))
                    matches.add(player);
            }
        }

        return matches.toArray(new ModulePlayer[0]);
    }

    /**
     * Get a player by its name
     *
     * @param name the player name
     * @return the module player
     */
    public ModulePlayer[] getPlayerStrict(final String name) {
        List<ModulePlayer> matches = new ArrayList<>();
        Collection<ModulePlayer> players = getOnlinePlayers();

        for (ModulePlayer player : players) {
            if (player.getName().equals(name)) {
                if (!matches.contains(player))
                    matches.add(player);
            }
        }

        return matches.toArray(new ModulePlayer[0]);
    }
}
