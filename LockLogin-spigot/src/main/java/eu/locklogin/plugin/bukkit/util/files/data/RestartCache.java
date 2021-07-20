package eu.locklogin.plugin.bukkit.util.files.data;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.JarManager;
import eu.locklogin.plugin.bukkit.LockLogin;
import eu.locklogin.plugin.bukkit.plugin.bungee.data.BungeeDataStorager;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RestartCache {

    private final KarmaFile cache = new KarmaFile(LockLogin.plugin, "plugin.cache", "plugin", "updater", "cache");

    /**
     * Store the sessions into the cache file
     */
    public final void storeUserData() {
        if (!cache.exists())
            cache.create();

        Map<UUID, ClientSession> sessions = User.getSessionMap();
        Map<UUID, GameMode> spectators = User.getSpectatorMap();
        String sessions_serialized = StringUtils.serialize(sessions);
        String spectators_serialized = StringUtils.serialize(spectators);

        if (sessions_serialized != null) {
            cache.set("SESSIONS", sessions_serialized);
        } else {
            Console.send(LockLogin.plugin, LockLogin.properties.getProperty("plugin_error_cache_save", "Failed to save cache object {0} ( {1} )"), Level.GRAVE, "sessions", "sessions are null");
        }

        if (spectators_serialized != null) {
            cache.set("SPECTATORS", spectators_serialized);
        } else {
            Console.send(LockLogin.plugin, LockLogin.properties.getProperty("plugin_error_cache_save", "Failed to save cache object {0} ( {1} )"), Level.GRAVE, "temp spectators", "spectators are null");
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
            Class<?> storagerClass = BungeeDataStorager.class;
            Field ownerField = storagerClass.getDeclaredField("proxyKey");

            String owner = (String) ownerField.get(null);
            cache.set("KEY", owner);
        } catch (Throwable ignored) {
        }

        try {
            Class<?> storagerClass = BungeeDataStorager.class;
            Field proxyField = storagerClass.getDeclaredField("proxies");

            Object proxies = proxyField.get(null);
            String serialized = StringUtils.serialize(proxies);

            if (serialized != null)
                cache.set("PROXIES", serialized);
        } catch (Throwable ignored) {
        }

        try {
            Class<?> storagerClass = BungeeDataStorager.class;
            Field bungeeField = storagerClass.getDeclaredField("multiBungee");

            boolean multiple = (boolean) bungeeField.get(null);

            cache.set("MULTIBUNGEE", multiple);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Load the stored sessions
     */
    public final void loadUserData() {
        if (cache.exists()) {
            String sessions_serialized = cache.getString("SESSIONS", "");
            String spectator_serialized = cache.getString("SPECTATORS", "");

            if (!sessions_serialized.replaceAll("\\s", "").isEmpty()) {
                Map<UUID, ClientSession> sessions = StringUtils.loadUnsafe(sessions_serialized);
                Map<UUID, ClientSession> fixedSessions = new HashMap<>();
                if (sessions != null) {
                    //Remove offline player sessions to avoid security issues
                    for (UUID id : sessions.keySet()) {
                        ClientSession session = sessions.getOrDefault(id, null);
                        if (session != null) {
                            OfflinePlayer player = LockLogin.plugin.getServer().getOfflinePlayer(id);
                            if (player.isOnline() && player.getPlayer() != null) {
                                fixedSessions.put(id, session);
                            }
                        }
                    }

                    try {
                        JarManager.changeField(User.class, "sessions", fixedSessions);
                    } catch (Throwable ex) {
                        Console.send(LockLogin.plugin, LockLogin.properties.getProperty("plugin_error_cache_load", "Failed to load cache object {0} ( {1} )"), Level.GRAVE, "sessions", ex.fillInStackTrace());
                    }
                } else {
                    Console.send(LockLogin.plugin, LockLogin.properties.getProperty("plugin_error_cache_load", "Failed to load cache object {0} ( {1} )"), Level.GRAVE, "sessions", "session map is null");
                }
            }

            if (!spectator_serialized.replaceAll("\\s", "").isEmpty()) {
                Map<UUID, GameMode> spectators = StringUtils.loadUnsafe(spectator_serialized);
                Map<UUID, GameMode> fixedSpectators = new HashMap<>();
                if (spectators != null) {
                    //Remove offline player sessions to avoid security issues
                    for (UUID id : spectators.keySet()) {
                        GameMode mode = spectators.getOrDefault(id, GameMode.SURVIVAL);
                        OfflinePlayer player = LockLogin.plugin.getServer().getOfflinePlayer(id);
                        if (player.isOnline() && player.getPlayer() != null) {
                            fixedSpectators.put(id, mode);
                        }
                    }

                    try {
                        JarManager.changeField(User.class, "temp_spectator", fixedSpectators);
                    } catch (Throwable ex) {
                        Console.send(LockLogin.plugin, LockLogin.properties.getProperty("plugin_error_cache_load", "Failed to load cache object {0} ( {1} )"), Level.GRAVE, "temp spectators", ex.fillInStackTrace());
                    }
                } else {
                    Console.send(LockLogin.plugin, LockLogin.properties.getProperty("plugin_error_cache_load", "Failed to load cache object {0} ( {1} )"), Level.GRAVE, "temp spectators", "temp spectators map is null");
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
                    JarManager.changeField(BungeeDataStorager.class, "proxyKey", key);
                }
            } catch (Throwable ignored) {
            }

            try {
                String proxies = cache.getString("PROXIES", "");

                if (!proxies.replaceAll("\\s", "").isEmpty()) {
                    Map<String, String> map = StringUtils.loadUnsafe(proxies);

                    if (map != null)
                        JarManager.changeField(BungeeDataStorager.class, "proxies", map);
                }
            } catch (Throwable ignored) {}

            try {
                boolean multiple = cache.getBoolean("MULTIBUNGEE", false);

                JarManager.changeField(BungeeDataStorager.class, "multiBungee", multiple);
            } catch (Throwable ignored) {}
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
