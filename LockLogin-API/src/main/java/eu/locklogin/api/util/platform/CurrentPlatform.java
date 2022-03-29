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
import eu.locklogin.api.account.param.AccountConstructor;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.server.TargetServer;
import eu.locklogin.api.util.enums.Manager;
import ml.karmaconfigs.api.common.Logger;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.loader.BruteLoader;
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

    private final static Map<UUID, Object> players = new ConcurrentHashMap<>();
    private final static ModuleServer server = new ModuleServer();
    private final static Logger logger = new Logger(APISource.loadProvider("LockLogin"));

    //private static Server remoteServer;
    private static Platform platform;
    private static Class<?> main;
    private static String prefix = "$";
    private static Class<? extends AccountManager> manager;
    private static Class<? extends AccountManager> default_manager;
    private static Class<? extends AccountManager> last_manager;
    private static Class<? extends ClientSession> sessionManager;
    private static PluginConfiguration current_config;
    private static ProxyConfiguration current_proxy;
    private static PluginMessages current_messages;
    @SuppressWarnings("all")
    private static BruteLoader current_appender;
    private static Runnable onDataContainerUpdate;
    private static boolean online;

    /**
     * Set the current account manager
     *
     * @param clazz the account manager class
     */
    public static void setAccountsManager(final Class<? extends AccountManager> clazz) {
        if (default_manager == null)
            default_manager = manager;

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
     * Connect a new player to the plugin
     *
     * @param mp     the module player
     * @param player the player
     */
    public static void connectPlayer(final ModulePlayer mp, final Object player) {
        players.put(mp.getUUID(), player);
        server.connectPlayer(mp);
    }

    /**
     * Disconnect a player from the plugin
     *
     * @param mp the player
     */
    public static void disconnectPlayer(final ModulePlayer mp) {
        players.remove(mp.getUUID());
        server.disconnectPlayer(mp);
    }

    /**
     * Add a server to the module server servers
     *
     * @param sv the server to add
     */
    public static void detectServer(final TargetServer sv) {
        server.addServer(sv);
    }

    /**
     * Set the current remote server
     *
     * @param remote the current remote server
     */
    public static void setRemoteServer(final Object remote) {
        /*if (remoteServer == null) {
            remoteServer = remote;
        }*/
    }

    /**
     * Get the LockLogin logger
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Get the current LockLogin server
     *
     * @return the current LockLogin server
     */
    public static ModuleServer getServer() {
        return server;
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
     * Set the current platform
     *
     * @param platform the platform
     */
    public static void setPlatform(final Platform platform) {
        CurrentPlatform.platform = platform;
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
     * Set the current main class
     *
     * @param main the current main class
     */
    public static void setMain(final Class<?> main) {
        CurrentPlatform.main = main;
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
     * @param type the account manager type
     * @param parameter the account manager player parameter
     * @return the current account manager
     *
     * @throws IllegalStateException if the manager type is unknown
     */
    @Nullable
    public static AccountManager getAccountManager(final Manager type, final @Nullable AccountConstructor<?> parameter) throws IllegalStateException {
        Class<? extends AccountManager> clazz;
        switch (type) {
            case DEFAULT:
                clazz = default_manager;
                break;
            case CUSTOM:
                if (manager != null) {
                    clazz = manager;
                } else {
                    clazz = default_manager;
                }
                break;
            case PREVIOUS:
                if (last_manager != null) {
                    clazz = last_manager;
                } else {
                    if (manager != null) {
                        clazz = manager;
                    } else {
                        clazz = default_manager;
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unexpected account manager type: " + type.name());
        }

        try {
            Constructor<? extends AccountManager> constructor = clazz.getDeclaredConstructor(AccountConstructor.class);
            constructor.setAccessible(true);
            return constructor.newInstance(parameter);
        } catch (Throwable ex) {
            try {
                Constructor<? extends AccountManager> constructor = clazz.getConstructor(AccountConstructor.class);
                constructor.setAccessible(true);
                return constructor.newInstance(parameter);
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
     *
     * @deprecated Deprecated as of 1.13.9, use {@link CurrentPlatform#getAccountManager(Manager, AccountConstructor)}
     */
    @Nullable
    @Deprecated
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
     * Get the default account manager
     *
     * @param constructorParams the constructor parameters types
     * @param parameters        the constructor parameter objects
     * @return the current account manager
     *
     * @deprecated Deprecated as of 1.13.9, use {@link CurrentPlatform#getAccountManager(Manager, AccountConstructor)}
     */
    @Nullable
    @Deprecated
    public static AccountManager getDefaultAccountManager(final Class<?>[] constructorParams, final Object... parameters) {
        try {
            Constructor<? extends AccountManager> constructor = default_manager.getDeclaredConstructor(constructorParams);
            constructor.setAccessible(true);
            return constructor.newInstance(parameters);
        } catch (Throwable ex) {
            try {
                Constructor<? extends AccountManager> constructor = default_manager.getConstructor(constructorParams);
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
     *
     * @deprecated Deprecated as of 1.13.9, use {@link CurrentPlatform#getAccountManager(Manager, AccountConstructor)}
     */
    @Nullable
    @Deprecated
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
     * Set the current server online mode
     *
     * @param onlineMode the online mode
     */
    public static void setOnline(final boolean onlineMode) {
        online = onlineMode;
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
     * Set the current module commands prefix
     *
     * @param modulePrefix the module commands prefix
     */
    public static void setPrefix(final String modulePrefix) {
        prefix = modulePrefix;
    }

    /**
     * Get the plugin appender
     *
     * @return the plugin appender
     */
    public static BruteLoader getPluginAppender() {
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

    /**
     * Get the remote messaging server
     *
     * @return the remote messaging server
     */
    @Nullable
    public static Object getRemoteServer() {
        return null;
    }
}
