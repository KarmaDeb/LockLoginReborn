package ml.karmaconfigs.locklogin.plugin.bungee.plugin;

import ml.karmaconfigs.api.bungee.Console;
import ml.karmaconfigs.api.bungee.karmayaml.FileCopy;
import ml.karmaconfigs.api.bungee.timer.AdvancedPluginTimer;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bungee.Main;
import ml.karmaconfigs.locklogin.plugin.bungee.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.bungee.listener.ChatListener;
import ml.karmaconfigs.locklogin.plugin.bungee.listener.JoinListener;
import ml.karmaconfigs.locklogin.plugin.bungee.listener.MessageListener;
import ml.karmaconfigs.locklogin.plugin.bungee.listener.QuitListener;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.bungee.plugin.sender.DataType;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.client.PlayerFile;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.Config;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.data.RestartCache;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.data.lock.LockedAccount;
import ml.karmaconfigs.locklogin.plugin.bungee.util.filter.ConsoleFilter;
import ml.karmaconfigs.locklogin.plugin.bungee.util.filter.PluginFilter;
import ml.karmaconfigs.locklogin.plugin.bungee.util.player.Session;
import ml.karmaconfigs.locklogin.plugin.common.security.client.Proxy;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import ml.karmaconfigs.locklogin.plugin.common.utils.ASCIIArtGenerator;
import ml.karmaconfigs.locklogin.plugin.common.web.AlertSystem;
import ml.karmaconfigs.locklogin.plugin.common.web.VersionChecker;
import ml.karmaconfigs.locklogin.plugin.common.web.VersionDownloader;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.reflections.Reflections;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import static ml.karmaconfigs.api.common.Console.Colors.YELLOW_BRIGHT;
import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.*;

public final class Manager {

    private static int changelog_requests = 0;
    private static int updater_id = 0;
    private static int alert_id = 0;

    public static void initialize() {
        int size = 10;
        String character = "*";
        try {
            size = Integer.parseInt(properties.getProperty("ascii_art_size", "10"));
            character = properties.getProperty("ascii_art_character", "*").substring(0, 1);
        } catch (Throwable ignored) {
        }

        System.out.println();
        artGen.print(YELLOW_BRIGHT, "LockLogin", size, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, character);
        Console.send("&eversion:&6 {0}", versionID);

        Proxy.scan();

        PlayerFile.migrateV1();
        PlayerFile.migrateV2();

        setupFiles();
        registerCommands();
        registerListeners();

        Console.send(" ");
        Console.send("&e-----------------------");

        if (!CurrentPlatform.isValidAccountManager()) {
            CurrentPlatform.setAccountsManager(PlayerFile.class);
            Console.send(plugin, "Loaded native player account manager", Level.INFO);
        } else {
            Console.send(plugin, "Loaded custom player account manager", Level.INFO);
        }
        if (!CurrentPlatform.isValidSessionManager()) {
            CurrentPlatform.setSessionManager(Session.class);
            Console.send(plugin, "Loaded native player session manager", Level.INFO);
        } else {
            Console.send(plugin, "Loaded custom player session manager", Level.INFO);
        }

        loadCache();

        plugin.getProxy().registerChannel(DataSender.CHANNEL_PLAYER);
        plugin.getProxy().registerChannel(DataSender.PLUGIN_CHANNEL);

        AccountManager manager = CurrentPlatform.getAccountManager(null);
        if (manager != null) {
            Set<AccountManager> accounts = manager.getAccounts();
            Set<AccountManager> nonLocked = new HashSet<>();
            for (AccountManager account : accounts) {
                LockedAccount locked = new LockedAccount(account.getUUID());
                if (!locked.getData().isLocked())
                    nonLocked.add(account);
            }

            SessionDataContainer.setRegistered(nonLocked.size());

            SessionDataContainer.onDataChange(data -> {
                try {
                    Collection<ServerInfo> servers = plugin.getProxy().getServers().values();

                    switch (data.getDataType()) {
                        case LOGIN:
                            for (ServerInfo server : servers) {
                                DataSender.send(server, DataSender.getBuilder(DataType.LOGGED, DataSender.PLUGIN_CHANNEL).addIntData(SessionDataContainer.getLogged()).build());
                            }
                            break;
                        case REGISTER:
                            for (ServerInfo server : servers) {
                                DataSender.send(server, DataSender.getBuilder(DataType.REGISTERED, DataSender.PLUGIN_CHANNEL).addIntData(SessionDataContainer.getRegistered()).build());
                            }
                            break;
                        default:
                            break;
                    }
                } catch (Throwable ignored) {
                }
            });
        }
    }

    public static void terminate() {
        try {
            Console.send(plugin, "Finalizing console filter, please wait", Level.INFO);
            Logger coreLogger = (Logger) LogManager.getRootLogger();

            Iterator<Filter> filters = coreLogger.getFilters();
            if (filters != null) {
                while (filters.hasNext()) {
                    Filter filter = filters.next();
                    if (filter.getClass().isAnnotationPresent(PluginFilter.class))
                        filter.stop();
                }
            }
        } catch (Throwable ignored) {
        }

        int size = 10;
        String character = "*";
        try {
            size = Integer.parseInt(properties.getProperty("ascii_art_size", "10"));
            character = properties.getProperty("ascii_art_character", "*").substring(0, 1);
        } catch (Throwable ignored) {
        }

        System.out.println();
        artGen.print(ml.karmaconfigs.api.common.Console.Colors.RED_BRIGHT, "LockLogin", size, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, character);
        Console.send("&eversion:&6 {0}", versionID);
        Console.send(" ");
        Console.send("&e-----------------------");
    }

    /**
     * Register plugin commands
     */
    protected static void registerCommands() {
        Set<String> unregistered = new LinkedHashSet<>();
        Set<String> registered = new HashSet<>();

        Reflections reflections = new Reflections("ml.karmaconfigs.locklogin.plugin.bungee.command");
        Set<Class<?>> commands = reflections.getTypesAnnotatedWith(SystemCommand.class);

        for (Class<?> clazz : commands) {
            try {
                String command = SystemCommand.manager.getDeclaredCommand(clazz);

                if (command != null && !command.replaceAll("\\s", "").isEmpty()) {
                    Object instance = clazz.getDeclaredConstructor(String.class).newInstance(command);

                    if (instance instanceof Command) {
                        Command executor = (Command) instance;
                        plugin.getProxy().getPluginManager().registerCommand(plugin, executor);

                        registered.add("/" + command);
                        for (String alias : executor.getAliases())
                            registered.add("/" + alias);
                    } else {
                        unregistered.add(command);
                    }
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        if (!unregistered.isEmpty()) {
            Console.send(plugin, properties.getProperty("command_register_problem", "Failed to register command(s): {0}"), Level.GRAVE, setToString(unregistered));
            Console.send(plugin, properties.getProperty("plugin_error_disabling", "Disabling plugin due an internal error"), Level.INFO);
        } else {
            Console.send(plugin, properties.getProperty("plugin_filter_initialize", "Initializing console filter to protect user data"), Level.INFO);

            try {
                ConsoleFilter filter = new ConsoleFilter(registered);

                Logger coreLogger = (Logger) LogManager.getRootLogger();
                coreLogger.addFilter(filter);
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
                logger.scheduleLog(Level.INFO, "Failed to register console filter");

                Console.send(plugin, properties.getProperty("plugin_filter_error", "An error occurred while initializing console filter, check logs for more info"), Level.GRAVE);
                Console.send(plugin, properties.getProperty("plugin_error_disabling", "Disabling plugin due an internal error"), Level.INFO);

            }
        }
    }

    /**
     * Setup the plugin files
     */
    protected static void setupFiles() {
        Set<String> failed = new LinkedHashSet<>();

        File cfg = new File(plugin.getDataFolder(), "config.yml");

        FileCopy config_copy = new FileCopy(plugin, "cfg/config.yml");
        try {
            config_copy.copy(cfg);
        } catch (Throwable ex) {
            failed.add("config.yml");
        }

        Config config = new Config();
        CurrentPlatform.setConfigManager(config);

        String country = config.getLang().country(config.getLangName());
        File msg_file = new File(plugin.getDataFolder() + File.separator + "lang" + File.separator + "v2", "messages_" + country + ".yml");

        InputStream internal = Main.class.getResourceAsStream("/lang/messages_" + country + ".yml");
        //Check if the file exists inside the plugin as an official language
        if (internal != null) {
            if (!msg_file.exists()) {
                FileCopy copy = new FileCopy(plugin, "lang/messages_" + country + ".yml");

                try {
                    copy.copy(msg_file);
                } catch (Throwable ex) {
                    failed.add(msg_file.getName());
                }
            }
        } else {
            if (!msg_file.exists()) {
                failed.add(msg_file.getName());
                Console.send(plugin, "Could not find community message pack named {0} in lang_v2 folder, using messages english as default", Level.GRAVE, msg_file.getName());

                msg_file = new File(plugin.getDataFolder() + File.separator + "lang" + File.separator + "v2", "messages_en.yml");

                if (!msg_file.exists()) {
                    FileCopy copy = new FileCopy(plugin, "lang/messages_en.yml");

                    try {
                        copy.copy(msg_file);
                    } catch (Throwable ex) {
                        failed.add(msg_file.getName());
                    }
                }
            } else {
                Console.send(plugin, "Detected community language pack, please make sure this pack is updated to avoid translation errors", Level.WARNING);
            }
        }

        if (!failed.isEmpty()) {
            Console.send(plugin, properties.getProperty("file_register_problem", "Failed to setup/check file(s): {0}. The plugin will use defaults, you can try to create files later by running /locklogin reload"), Level.WARNING, setToString(failed));
        }

        Config.manager.checkValues();
    }

    /**
     * Register the plugin listeners
     */
    protected static void registerListeners() {
        Listener onJoin = new JoinListener();
        Listener onQuit = new QuitListener();
        Listener onChat = new ChatListener();
        Listener onMessage = new MessageListener();

        plugin.getProxy().getPluginManager().registerListener(plugin, onJoin);
        plugin.getProxy().getPluginManager().registerListener(plugin, onQuit);
        plugin.getProxy().getPluginManager().registerListener(plugin, onChat);
        plugin.getProxy().getPluginManager().registerListener(plugin, onMessage);
    }

    /**
     * Load the plugin cache if exists
     */
    protected static void loadCache() {
        RestartCache cache = new RestartCache();
        cache.loadBungeeKey();
        cache.loadUserData();

        cache.remove();
    }

    /**
     * Perform a version check
     */
    protected static void performVersionCheck() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        VersionChecker checker = new VersionChecker(versionID);
        checker.checkVersion(config.getUpdaterOptions().getChannel());

        if (checker.isOutdated()) {
            if (changelog_requests <= 0) {
                changelog_requests = 3;

                Console.send(checker.getChangelog());

                if (!VersionDownloader.isDownloading()) {
                    VersionDownloader downloader = new VersionDownloader(versionID, config.getUpdaterOptions().getChannel());
                    downloader.download(
                            file -> Console.send(plugin, properties.getProperty("updater_downloaded", "Downloaded latest version plugin instance, to apply the updates run /locklogin applyUpdates"), Level.INFO),
                            error -> {
                                if (error != null) {
                                    logger.scheduleLog(Level.GRAVE, error);
                                    logger.scheduleLog(Level.INFO, "Failed to download latest LockLogin instance");
                                    Console.send(plugin, properties.getProperty("updater_download_fail", "Failed to download latest LockLogin update ( {0} )"), Level.INFO, error.fillInStackTrace());
                                }
                            });
                }
            } else {
                changelog_requests--;
            }
        }
    }

    /**
     * Schedule the version check process
     */
    protected static void scheduleVersionCheck() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        AdvancedPluginTimer timer = new AdvancedPluginTimer(plugin, config.getUpdaterOptions().getInterval(), true).setAsync(true).addActionOnEnd(Manager::performVersionCheck);
        if (config.getUpdaterOptions().isEnabled())
            timer.start();

        updater_id = timer.getTimerId();

    }

    /**
     * Schedule the alert system
     */
    protected static void scheduleAlertSystem() {
        AdvancedPluginTimer timer = new AdvancedPluginTimer(plugin, 30, true).setAsync(true).addActionOnEnd(() -> {
            AlertSystem system = new AlertSystem();
            system.checkAlerts();

            if (system.available())
                Console.send(system.getMessage());
        });
        timer.start();

        alert_id = timer.getTimerId();

    }

    /**
     * Restart the version checker
     */
    public static void restartVersionChecker() {
        try {
            AdvancedPluginTimer timer = AdvancedPluginTimer.getManager.getTimer(updater_id);
            timer.setCancelled();
        } catch (Throwable ignored) {
        }

        scheduleVersionCheck();
    }

    /**
     * Restart the alert system timer
     */
    public static void restartAlertSystem() {
        try {
            AdvancedPluginTimer timer = AdvancedPluginTimer.getManager.getTimer(alert_id);
            timer.setCancelled();
        } catch (Throwable ignored) {
        }

        scheduleAlertSystem();
    }

    /**
     * Convert a set of strings into a single string
     *
     * @param set the set to convert
     * @return the converted set
     */
    private static String setToString(final Set<String> set) {
        StringBuilder builder = new StringBuilder();
        for (String str : set) {
            builder.append(str.replace(",", "comma")).append(", ");
        }

        return StringUtils.replaceLast(builder.toString(), ", ", "");
    }
}
