package eu.locklogin.plugin.bungee.util.files.data;

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
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.file.element.types.ElementPrimitive;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static eu.locklogin.plugin.bungee.LockLogin.*;

public final class RestartCache {

    private final KarmaMain cache = new KarmaMain(plugin, "plugin.cache", "plugin", "updater", "cache");

    /**
     * Store the sessions into the cache file
     */
    public void storeUserData() {
        Map<UUID, ClientSession> sessions = User.getSessionMap();
        String sessions_serialized = StringUtils.serialize(sessions);

        if (sessions_serialized != null) {
            cache.setRaw("sessions", sessions_serialized);
        } else {
            console.send(properties.getProperty("plugin_error_cache_save", "Failed to save cache object {0} ( {1} )"), Level.GRAVE, "sessions", "sessions are null");
        }

        cache.save();
    }

    /**
     * Load the stored sessions
     */
    public void loadUserData() {
        if (cache.exists() && cache.isSet("sessions")) {
            Element<?> element = cache.get("sessions");
            if (element.isPrimitive()) {
                ElementPrimitive primitive = element.getAsPrimitive();
                if (primitive.isString()) {
                    String sessions_serialized = primitive.asString();

                    if (!StringUtils.isNullOrEmpty(sessions_serialized)) {
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
                                JarManager.changeField(User.class, "sessions", fixedSessions);
                            } catch (Throwable ex) {
                                console.send(properties.getProperty("plugin_error_cache_load", "Failed to load cache object {0} ( {1} )"), Level.GRAVE, "sessions", ex.fillInStackTrace());
                            }
                        } else {
                            console.send(properties.getProperty("plugin_error_cache_load", "Failed to load cache object {0} ( {1} )"), Level.GRAVE, "sessions", "session map is null");
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove the cache file
     */
    public void remove() {
        try {
            cache.delete();
        } catch (Throwable ignored) {
        }
    }
}
