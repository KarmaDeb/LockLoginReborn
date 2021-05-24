package ml.karmaconfigs.locklogin.plugin.bungee.util.files.data;

import ml.karmaconfigs.api.bungee.Console;
import ml.karmaconfigs.api.bungee.KarmaFile;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.plugin.bungee.listener.MessageListener;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.JarManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.plugin;
import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.properties;

public final class RestartCache {

    private final KarmaFile cache = new KarmaFile(plugin, "plugin.cache", "plugin", "updater", "cache");

    /**
     * Store the sessions into the cache file
     */
    public final void storeUserData() {
        if (!cache.exists())
            cache.create();

        Map<UUID, ClientSession> sessions = User.getSessionMap();
        String sessions_serialized = StringUtils.serialize(sessions);

        if (sessions_serialized != null) {
            cache.set("SESSIONS", sessions_serialized);
        } else {
            Console.send(plugin, properties.getProperty("plugin_error_cache_save", "Failed to save cache object {0} ( {1} )"), Level.GRAVE, "sessions", "sessions are null");
        }
    }

    /**
     * Store bungeecord key so a fake bungeecord server
     * won't be able to send a fake key
     */
    public final void storeBungeeKey() {
        if (!cache.exists())
            cache.create();

        try {
            Class<?> storagerClass = DataSender.class;
            Field keyField = storagerClass.getDeclaredField("key");

            String key = (String) keyField.get(null);
            cache.set("KEY", key);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Load the stored sessions
     */
    public final void loadUserData() {
        if (cache.exists()) {
            String sessions_serialized = cache.getString("SESSIONS", "");

            if (!sessions_serialized.replaceAll("\\s", "").isEmpty()) {
                Map<UUID, ClientSession> sessions = StringUtils.loadUnsafe(sessions_serialized);
                Map<UUID, ClientSession> fixedSessions = new HashMap<>();
                if (sessions != null) {
                    //Remove offline player sessions to avoid security issues
                    for (UUID id : sessions.keySet()) {
                        ClientSession session = sessions.getOrDefault(id, null);
                        if (session != null) {
                            ProxiedPlayer player = plugin.getProxy().getPlayer(id);

                            if (player != null && player.isConnected()) {
                                fixedSessions.put(id, session);
                            }
                        }
                    }

                    try {
                        JarManager.changeField(User.class, "sessions", true, fixedSessions);
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
     * Load the stored bungeecord key
     */
    public final void loadBungeeKey() {
        if (cache.exists()) {
            try {
                String key = cache.getString("KEY", "");

                if (!key.replaceAll("\\s", "").isEmpty()) {
                    JarManager.changeField(DataSender.class, "key", true, key);
                }
            } catch (Throwable ignored) {
            }

            try {
                String token = cache.getString("TOKEN", "");

                if (!token.replaceAll("\\s", "").isEmpty()) {
                    JarManager.changeField(MessageListener.class, "token", true, token);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Remove the cache file
     */
    public final void remove() {
        try {
            Files.delete(cache.getFile().toPath());
        } catch (Throwable ignored) {
        }
    }
}
