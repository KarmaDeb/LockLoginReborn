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

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.account.param.AccountConstructor;
import eu.locklogin.api.encryption.libraries.sha.SHA512;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.server.TargetServer;
import eu.locklogin.api.premium.PremiumDatabase;
import eu.locklogin.api.security.LockLoginRuntime;
import eu.locklogin.api.security.backup.BackupScheduler;
import eu.locklogin.api.util.enums.ManagerType;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.KarmaPrimitive;
import ml.karmaconfigs.api.common.karma.file.element.types.Element;
import ml.karmaconfigs.api.common.karma.loader.BruteLoader;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.logger.KarmaLogger;
import ml.karmaconfigs.api.common.logger.Logger;
import ml.karmaconfigs.api.common.string.random.RandomString;
import ml.karmaconfigs.api.common.string.text.TextContent;
import ml.karmaconfigs.api.common.string.text.TextType;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    private final static KarmaLogger logger = new Logger(APISource.loadProvider("LockLogin"));

    //Unique server hash, for panel utilities and more...
    private static final String server_hash;

    static {
        //Make server hash
        String hash;
        KarmaSource plugin = APISource.getOriginal(false);
        try {
            KarmaMain kf = new KarmaMain(plugin, "server.cfg", "cache", "locklogin");

            Element<?> tmp_hash = kf.get("hash");
            if (!tmp_hash.isPrimitive() || !tmp_hash.getAsPrimitive().isString()) {
                tmp_hash = new KarmaPrimitive(Base64.getEncoder().encodeToString(new SHA512().hash(new RandomString(
                        RandomString.createBuilder()
                                .withContent(TextContent.NUMBERS_AND_LETTERS)
                                .withType(TextType.RANDOM_SIZE)
                                .withSize(64)
                ).create()).getBytes(StandardCharsets.UTF_8)));

                kf.set("hash", tmp_hash);
                kf.save();

                plugin.console().send("Created new hash", Level.INFO);
            }

            hash = tmp_hash.getAsString();
            plugin.console().send("Loaded server hash", Level.INFO);
        } catch (Throwable ex) {
            plugin.logger().scheduleLog(Level.GRAVE, ex);
            plugin.logger().scheduleLog(Level.INFO, "Failed to load server hash");

            hash = Base64.getEncoder().encodeToString(new SHA512().hash(new RandomString(
                    RandomString.createBuilder()
                            .withContent(TextContent.NUMBERS_AND_LETTERS)
                            .withType(TextType.RANDOM_SIZE)
                            .withSize(64)
            ).create()).getBytes(StandardCharsets.UTF_8));

            plugin.console().send("Using temporal server hash due an error", Level.GRAVE);
        }

        server_hash = hash;
    }

    //private static Server remoteServer;
    private static Platform platform;
    private static Class<?> main;
    private static String prefix = "$";
    private static Class<? extends eu.locklogin.api.account.AccountManager> manager;
    private static Class<? extends eu.locklogin.api.account.AccountManager> default_manager;
    private static Class<? extends eu.locklogin.api.account.AccountManager> last_manager;
    private static Class<? extends ClientSession> sessionManager;

    private static BackupScheduler backupScheduler;
    private static PremiumDatabase premiumDatabase;
    @SuppressWarnings("unused")
    private static PluginConfiguration fake_config;
    @SuppressWarnings("unused")
    private static ProxyConfiguration fake_proxy;

    private static PluginConfiguration current_config;
    private static ProxyConfiguration current_proxy;
    private static PluginMessages current_messages;
    @SuppressWarnings("all")
    private static BruteLoader current_appender;
    private static Runnable onDataContainerUpdate;
    private static boolean online;

    /**
     * Initialize the server
     *
     * @param port the server port
     */
    public static void init(final int port) {
        server.port = port;
    }

    /**
     * Set the current account manager
     *
     * @param clazz the account manager class
     */
    public static void setAccountsManager(final Class<? extends eu.locklogin.api.account.AccountManager> clazz) {
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
     * Set the backup scheduler
     *
     * @param scheduler the backup scheduler
     */
    public static void setBackupScheduler(final BackupScheduler scheduler) {
        backupScheduler = scheduler;
    }

    /**
     * Set the premium database
     *
     * @param database the premium database
     */
    public static void setPremiumDatabase(final PremiumDatabase database) {
        premiumDatabase = database;
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
        LockLoginRuntime.checkSecurity(true);

        players.put(mp.getUUID(), player);
        server.connectPlayer(mp);
    }

    /**
     * Disconnect a player from the plugin
     *
     * @param mp the player
     */
    public static void disconnectPlayer(final ModulePlayer mp) {
        LockLoginRuntime.checkSecurity(true);

        players.remove(mp.getUUID());
        server.disconnectPlayer(mp);
    }

    /**
     * Add a server to the module server servers
     *
     * @param sv the server to add
     */
    @SuppressWarnings("unused")
    public static void detectServer(final TargetServer sv) {
        server.addServer(sv);
    }

    /**
     * Get the LockLogin logger
     */
    public static KarmaLogger getLogger() {
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
        return manager != null && eu.locklogin.api.account.AccountManager.class.isAssignableFrom(manager);
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
    public static eu.locklogin.api.account.AccountManager getAccountManager(final ManagerType type, final @Nullable AccountConstructor<?> parameter) throws IllegalStateException {
        Class<? extends eu.locklogin.api.account.AccountManager> clazz;
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
            Constructor<? extends eu.locklogin.api.account.AccountManager> constructor = clazz.getDeclaredConstructor(AccountConstructor.class);
            constructor.setAccessible(true);
            return constructor.newInstance(parameter);
        } catch (Throwable ex) {
            try {
                Constructor<? extends eu.locklogin.api.account.AccountManager> constructor = clazz.getConstructor(AccountConstructor.class);
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
     * @deprecated Deprecated as of 1.13.9, use {@link CurrentPlatform#getAccountManager(ManagerType, AccountConstructor)}
     */
    @Nullable
    @Deprecated
    public static eu.locklogin.api.account.AccountManager getAccountManager(final Class<?>[] constructorParams, final Object... parameters) {
        try {
            Constructor<? extends eu.locklogin.api.account.AccountManager> constructor = manager.getDeclaredConstructor(constructorParams);
            constructor.setAccessible(true);
            return constructor.newInstance(parameters);
        } catch (Throwable ex) {
            try {
                Constructor<? extends eu.locklogin.api.account.AccountManager> constructor = manager.getConstructor(constructorParams);
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
     * @deprecated Deprecated as of 1.13.9, use {@link CurrentPlatform#getAccountManager(ManagerType, AccountConstructor)}
     */
    @Nullable
    @Deprecated
    public static eu.locklogin.api.account.AccountManager getDefaultAccountManager(final Class<?>[] constructorParams, final Object... parameters) {
        try {
            Constructor<? extends eu.locklogin.api.account.AccountManager> constructor = default_manager.getDeclaredConstructor(constructorParams);
            constructor.setAccessible(true);
            return constructor.newInstance(parameters);
        } catch (Throwable ex) {
            try {
                Constructor<? extends eu.locklogin.api.account.AccountManager> constructor = default_manager.getConstructor(constructorParams);
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
     * @deprecated Deprecated as of 1.13.9, use {@link CurrentPlatform#getAccountManager(ManagerType, AccountConstructor)}
     */
    @Nullable
    @Deprecated
    public static eu.locklogin.api.account.AccountManager getLastAccountManager(final Class<?>[] constructorParams, final Object... parameters) {
        if (last_manager != null) {
            try {
                Constructor<? extends eu.locklogin.api.account.AccountManager> constructor = last_manager.getDeclaredConstructor(constructorParams);
                constructor.setAccessible(true);
                return constructor.newInstance(parameters);
            } catch (Throwable ex) {
                try {
                    Constructor<? extends eu.locklogin.api.account.AccountManager> constructor = last_manager.getConstructor(constructorParams);
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
     * Get the backup scheduler
     *
     * @return the backup scheduler
     */
    public static BackupScheduler getBackupScheduler() {
        return backupScheduler;
    }

    /**
     * Get the premium database
     *
     * @return the premium database
     */
    public static PremiumDatabase getPremiumDatabase() {
        return premiumDatabase;
    }

    /**
     * Get the current plugin configuration manager
     *
     * @return the current plugin configuration manager
     */
    public static PluginConfiguration getRealConfiguration() {
        return current_config;
    }

    /**
     * Get the current proxy configuration manager
     *
     * @return the current proxy configuration manager
     */
    public static ProxyConfiguration getRealProxyConfiguration() {
        return current_proxy;
    }

    /**
     * Get the current plugin configuration manager
     *
     * @return the current plugin configuration manager
     */
    public static PluginConfiguration getConfiguration() {
        return (fake_config != null ? fake_config : current_config);
    }

    /**
     * Get the current proxy configuration manager
     *
     * @return the current proxy configuration manager
     */
    public static ProxyConfiguration getProxyConfiguration() {
        return (fake_proxy != null ? fake_proxy : current_proxy);
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
    @SuppressWarnings("unused")
    public static Object getRemoteServer() {
        return null;
    }

    /**
     * Get the LockLogin generated server hash
     *
     * @return the LockLogin generated server hash
     */
    public static String getServerHash() {
        return server_hash;
    }
}
