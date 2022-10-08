package eu.locklogin.api.util.platform;

import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.server.TargetServer;

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
    private final static Set<TargetServer> servers = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
     * Connect a server
     *
     * @param server the server to add
     */
    void addServer(final TargetServer server) {
        servers.add(server);
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
     * Get the servers
     *
     * @return the servers
     */
    public Collection<TargetServer> getServers() {
        Set<TargetServer> unmodifiableSet = new LinkedHashSet<>(servers);
        Stream<TargetServer> stream = unmodifiableSet.stream().sorted(Comparator.comparing(TargetServer::getName));

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
     * Get if a player is online
     *
     * @param id the player id
     * @return if the player is online
     */
    public boolean isOnline(final UUID id) {
        Collection<ModulePlayer> players = getOnlinePlayers();
        for (ModulePlayer player : players) {
            if (player.getUUID().equals(id))
                return true;
        }

        return false;
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

    /**
     * Get if the player is online
     *
     * @param player the player
     * @return if the player is online
     */
    public boolean isValid(final ModulePlayer player) {
        return getOnlinePlayers().contains(player);
    }

    /**
     * Get a server
     *
     * @param name the target server
     * @return the server
     */
    @Nullable
    public TargetServer getServer(final String name) {
        TargetServer server = null;

        for (TargetServer registered : servers) {
            if (registered.getName().equalsIgnoreCase(name)) {
                server = registered;
                break;
            }
        }

        return server;
    }
}
