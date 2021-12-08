package eu.locklogin.plugin.velocity.util.player;

import com.velocitypowered.api.proxy.Player;

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
    static void insert(final Player player, final User user) {
        database.put(player.getUniqueId(), user);
    }

    /**
     * Remove a user
     *
     * @param player the user
     */
    public static void removeUser(final Player player) {
        database.remove(player.getUniqueId());
    }

    /**
     * Load an user
     *
     * @param player the user
     * @return the user
     */
    public static User loadUser(final Player player) {
        if (player != null) {
            return database.getOrDefault(player.getUniqueId(), null);
        }

        return null;
    }
}
