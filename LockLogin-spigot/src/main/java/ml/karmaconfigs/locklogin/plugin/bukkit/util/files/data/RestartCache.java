package ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.bukkit.KarmaFile;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

public final class RestartCache {

    private final KarmaFile cache = new KarmaFile(plugin, "plugin.cache", "restart", "cache");

    /**
     * Store the sessions into the cache file
     */
    public final void storeSessions() {
        if (!cache.exists())
            cache.create();

        Map<UUID, ClientSession> sessions = User.getSessionMap();
        String serialized = serialize(sessions);

        if (serialized != null) {
            cache.set("SESSIONS", serialized);
        } else {
            Console.send(plugin, properties.getProperty("plugin_error_cache_save", "Failed to save cache object {0} ( {1} )"), Level.GRAVE, "sessions", "sessions are null");
        }
    }

    /**
     * Store bungeecord data so the plugin won't need
     * to load it again after a server restart
     */
    public final void storeBungeeData() {
        if (!cache.exists())
            cache.create();


    }

    /**
     * Load the stored sessions
     */
    public final void loadSessions() {
        if (cache.exists()) {
            String serialized = cache.getString("SESSIONS", "");
            if (!serialized.replaceAll("\\s", "").isEmpty()) {
                Map<UUID, ClientSession> sessions = unSerializeMap(serialized);
                Map<UUID, ClientSession> fixedSessions = new HashMap<>();
                if (sessions != null) {
                    //Remove offline player sessions to avoid security issues
                    for (UUID id : sessions.keySet()) {
                        ClientSession session = sessions.getOrDefault(id, null);
                        if (session != null) {
                            OfflinePlayer player = plugin.getServer().getOfflinePlayer(id);
                            if (player.isOnline() && player.getPlayer() != null) {
                                fixedSessions.put(id, session);
                            }
                        }
                    }

                    try {
                        Class<?> user = User.class;
                        Field sessionsField = user.getDeclaredField("sessions");

                        sessionsField.setAccessible(true);
                        sessionsField.setInt(sessionsField, sessionsField.getModifiers() & ~Modifier.FINAL);
                        sessionsField.set(user, fixedSessions);
                        sessionsField.setInt(sessionsField, sessionsField.getModifiers() & Modifier.FINAL);

                    } catch (Throwable ex) {
                        Console.send(plugin, properties.getProperty("plugin_error_cache_load", "Failed to load cache object {0} ( {1} )"), Level.GRAVE, "sessions", ex.fillInStackTrace());
                    }
                } else {
                    Console.send(plugin, properties.getProperty("plugin_error_cache_load", "Failed to load cache object {0} ( {1} )"), Level.GRAVE, "sessions", "session map is null");
                }
            }
        }
    }

    /**
     * Remove the cache file
     */
    public final void remove() {
        try {
            Files.delete(cache.getFile().toPath());
        } catch (Throwable ignored) {}
    }

    /**
     * Serialize the object into a string
     *
     * @param object the object to serialize
     * @return the serialized object
     */
    @Nullable
    private String serialize(final Object object) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ObjectOutputStream obj_out = new ObjectOutputStream(output);
            obj_out.writeObject(object);
            obj_out.flush();
            return Base64.getEncoder().encodeToString(output.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Un-serialize the object
     *
     * @param serialized the serialized object
     * @param <K> the map key type
     * @param <V> the map value type
     * @return the un-serialized object
     */
    @Nullable
    private <K,V> Map<K, V> unSerializeMap(String serialized) {
        try {
            serialized = new String(Base64.getDecoder().decode(serialized.getBytes(StandardCharsets.UTF_8)));
            ByteArrayInputStream input = new ByteArrayInputStream(serialized.getBytes(StandardCharsets.UTF_8));
            ObjectInputStream obj_input = new ObjectInputStream(input);
            Object obj = obj_input.readObject();

            if (obj instanceof Map) {
                Map<K, V> returnMap = new HashMap<>();
                Map<?, ?> map = (Map<?, ?>) obj;

                for (Object key : map.keySet()) {
                    returnMap.put((K) key, (V) map.get(key));
                }

                return returnMap;
            }

            return null;
        } catch (Throwable ex) {
            return null;
        }
    }
}
