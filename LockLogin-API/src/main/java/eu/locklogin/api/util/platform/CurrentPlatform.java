package eu.locklogin.api.util.platform;

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

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.ProxyConfiguration;
import ml.karmaconfigs.api.common.karma.loader.JarAppender;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Current platform utilities
 */
public final class CurrentPlatform {

    private static Platform platform;
    private static Class<?> main;

    private static String prefix = "$";
    private static String karma_api = "1.2.4";

    private static Class<? extends AccountManager> manager;
    private static Class<? extends AccountManager> last_manager;

    private static Class<? extends ClientSession> sessionManager;

    private static PluginConfiguration current_config;
    private static ProxyConfiguration current_proxy;
    private static PluginMessages current_messages;

    @SuppressWarnings("all")
    private static JarAppender current_appender;

    private static Runnable onDataContainerUpdate;

    private static boolean online;

    private final static Map<UUID, Object> players = new ConcurrentHashMap<>();

    /**
     * Set the current account manager
     *
     * @param clazz the account manager class
     */
    public static void setAccountsManager(final Class<? extends AccountManager> clazz) {
        last_manager = manager;
        manager = clazz;
    }

    /**
     * Set the current client session manager
     *
     * @param clazz the client session class
     */
    public static void setSessionManager(final Class<? extends ClientSession> clazz) {
        sessionManager = clazz;
    }

    /**
     * Set the current plugin configuration manager
     *
     * @param configuration the current plugin configuration manager
     */
    public static void setConfigManager(final PluginConfiguration configuration) {
        current_config = configuration;
    }

    /**
     * Set the current proxy configuration manager
     *
     * @param proxy the current proxy configuration manager
     */
    public static void setProxyManager(final ProxyConfiguration proxy) {
        current_proxy = proxy;
    }

    /**
     * Set the current plugin messages
     *
     * @param messages the current plugin messages
     */
    public static void setPluginMessages(final PluginMessages messages) {
        current_messages = messages;
    }

    /**
     * Set the current KarmaAPI version
     *
     * @param version the current KarmaAPI version
     * @deprecated This is no longer needed as KarmaAPI
     * is included inside the plugin
     */
    @Deprecated
    public static void setKarmaAPI(final String version) {
        karma_api = version;
    }

    /**
     * Set the action to perform when session data container
     * requests update
     *
     * @param request the action to perform
     */
    public static void setOnDataContainerUpdate(final Runnable request) {
        onDataContainerUpdate = request;
    }

    /**
     * Request the session data container update
     */
    public static void requestDataContainerUpdate() {
        if (onDataContainerUpdate != null)
            onDataContainerUpdate.run();
    }

    /**
     * Set the current platform
     *
     * @param platform the platform
     */
    public static void setPlatform(final Platform platform) {
        CurrentPlatform.platform = platform;
    }

    /**
     * Set the current main class
     *
     * @param main the current main class
     */
    public static void setMain(final Class<?> main) {
        CurrentPlatform.main = main;
    }

    /**
     * Set the current server online mode
     *
     * @param onlineMode the online mode
     */
    public static void setOnline(final boolean onlineMode) {
        online = onlineMode;
    }

    /**
     * Set the current module commands prefix
     *
     * @param modulePrefix the module commands prefix
     */
    public static void setPrefix(final String modulePrefix) {
        prefix = modulePrefix;
    }

    /**
     * Connect a new player to the plugin
     *
     * @param id the player uuid
     * @param player the player
     */
    public static void connectPlayer(final UUID id, final Object player) {
        players.put(id, player);
    }

    /**
     * Disconnect a player from the plugin
     *
     * @param id the player uuid
     */
    public static void disconnectPlayer(final UUID id) {
        players.remove(id);
    }

    /**
     * Get the current platform
     *
     * @return the current platform
     */
    public static Platform getPlatform() {
        return platform;
    }

    /**
     * Get the current main class
     *
     * @return the current main class
     */
    public static Class<?> getMain() {
        return main;
    }

    /**
     * Check if the current account manager
     * is valid
     *
     * @return if the current account manager is valid
     */
    public static boolean isValidAccountManager() {
        return manager != null && AccountManager.class.isAssignableFrom(manager);
    }

    /**
     * Check if the current session manager
     * is valid
     *
     * @return if the current session manager is valid
     */
    public static boolean isValidSessionManager() {
        return sessionManager != null && ClientSession.class.isAssignableFrom(sessionManager);
    }

    /**
     * Get the current account manager
     *
     * @param constructorParams the constructor parameters types
     * @param parameters        the constructor parameter objects
     * @return the current account manager
     */
    @Nullable
    public static AccountManager getAccountManager(final Class<?>[] constructorParams, final Object... parameters) {
        try {
            Constructor<? extends AccountManager> constructor = manager.getDeclaredConstructor(constructorParams);
            constructor.setAccessible(true);
            return constructor.newInstance(parameters);
        } catch (Throwable ex) {
            try {
                Constructor<? extends AccountManager> constructor = manager.getConstructor(constructorParams);
                constructor.setAccessible(true);
                return constructor.newInstance(parameters);
            } catch (Throwable exc) {
                return null;
            }
        }
    }

    /**
     * Get the current account manager
     *
     * @param constructorParams the constructor parameters types
     * @param parameters        the constructor parameter objects
     * @return the current account manager
     */
    @Nullable
    public static AccountManager getLastAccountManager(final Class<?>[] constructorParams, final Object... parameters) {
        if (last_manager != null) {
            try {
                Constructor<? extends AccountManager> constructor = last_manager.getDeclaredConstructor(constructorParams);
                constructor.setAccessible(true);
                return constructor.newInstance(parameters);
            } catch (Throwable ex) {
                try {
                    Constructor<? extends AccountManager> constructor = last_manager.getConstructor(constructorParams);
                    constructor.setAccessible(true);
                    return constructor.newInstance(parameters);
                } catch (Throwable exc) {
                    return null;
                }
            }
        }

        return getAccountManager(constructorParams, parameters);
    }

    /**
     * Get the current account manager
     *
     * @param constructorParams the constructor parameters types
     * @param parameters        the constructor parameter objects
     * @return the current account manager
     */
    @SuppressWarnings("all")
    @Nullable
    public static ClientSession getSessionManager(final Class<?>[] constructorParams, final Object... parameters) {
        try {
            Constructor<? extends ClientSession> constructor = sessionManager.getDeclaredConstructor(constructorParams);
            constructor.setAccessible(true);
            return constructor.newInstance(parameters);
        } catch (Throwable ex) {
            try {
                Constructor<? extends ClientSession> constructor = sessionManager.getConstructor(constructorParams);
                constructor.setAccessible(true);
                return constructor.newInstance(parameters);
            } catch (Throwable exc) {
                return null;
            }
        }
    }

    /**
     * Get the current plugin configuration manager
     *
     * @return the current plugin configuration manager
     */
    public static PluginConfiguration getConfiguration() {
        return current_config;
    }

    /**
     * Get the current proxy configuration manager
     *
     * @return the current proxy configuration manager
     */
    public static ProxyConfiguration getProxyConfiguration() {
        return current_proxy;
    }

    /**
     * Get the current plugin messages
     *
     * @return the current plugin messages
     */
    public static PluginMessages getMessages() {
        return current_messages;
    }

    /**
     * Check if the current server is online
     * mode
     *
     * @return if the server is online mode
     */
    public static boolean isOnline() {
        return online;
    }

    /**
     * Get the plugin modules command prefix
     *
     * @return the command prefix
     */
    public static String getPrefix() {
        return prefix;
    }

    /**
     * Get the current KarmaAPI version
     *
     * @return the current KarmaAPI version
     */
    public static String getKarmaAPI() {
        return karma_api;
    }

    /**
     * Get the plugin appender
     *
     * @return the plugin appender
     */
    public static JarAppender getPluginAppender() {
        return current_appender;
    }

    /**
     * Get the connected players
     *
     * @return the connected players
     */
    public static Map<UUID, Object> getConnectedPlayers() {
        return new HashMap<>(players);
    }
}
