package eu.locklogin.plugin.bungee.util.player;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockLogin active user database
 */
public final class UserDatabase {

    private final static Map<UUID, User> database = new ConcurrentHashMap<>();

    /**
     * Insert the user
     *
     * @param player the player
     * @param user   the player user
     */
    static void insert(final ProxiedPlayer player, final User user) {
        database.put(player.getUniqueId(), user);
    }

    /**
     * Remove a user
     *
     * @param player the user
     */
    public static void removeUser(final ProxiedPlayer player) {
        database.remove(player.getUniqueId());
    }

    /**
     * Load an user
     *
     * @param player the user
     * @return the user
     */
    public static User loadUser(final ProxiedPlayer player) {
        if (player != null) {
            return database.getOrDefault(player.getUniqueId(), null);
        }

        return null;
    }
}
