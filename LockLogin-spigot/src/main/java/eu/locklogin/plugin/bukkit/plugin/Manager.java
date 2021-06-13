package eu.locklogin.plugin.bukkit.plugin;

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

import eu.locklogin.plugin.bukkit.Main;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.listener.*;
import eu.locklogin.plugin.bukkit.plugin.bungee.BungeeReceiver;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karmafile.karmayaml.FileCopy;
import ml.karmaconfigs.api.bukkit.reflections.BarMessage;
import ml.karmaconfigs.api.common.timer.AdvancedPluginTimer;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.UserHookEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserUnHookEvent;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleManager;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.util.LockLoginPlaceholder;
import eu.locklogin.plugin.bukkit.util.files.Config;
import eu.locklogin.plugin.bukkit.util.files.Message;
import eu.locklogin.plugin.bukkit.util.files.client.PlayerFile;
import eu.locklogin.plugin.bukkit.util.files.data.RestartCache;
import eu.locklogin.plugin.bukkit.util.files.data.lock.LockedAccount;
import eu.locklogin.plugin.bukkit.util.filter.ConsoleFilter;
import eu.locklogin.plugin.bukkit.util.filter.PluginFilter;
import eu.locklogin.plugin.bukkit.util.inventory.object.Button;
import eu.locklogin.plugin.bukkit.util.player.ClientVisor;
import eu.locklogin.plugin.bukkit.util.player.SessionCheck;
import eu.locklogin.api.common.security.client.IpData;
import eu.locklogin.api.common.security.client.Proxy;
import eu.locklogin.api.common.session.Session;
import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.common.session.SessionKeeper;
import eu.locklogin.api.common.utils.other.ASCIIArtGenerator;
import eu.locklogin.api.common.web.AlertSystem;
import eu.locklogin.api.common.web.VersionChecker;
import eu.locklogin.api.common.web.VersionDownloader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static ml.karmaconfigs.api.common.Console.Colors.YELLOW_BRIGHT;
import static eu.locklogin.plugin.bukkit.LockLogin.*;

public final class Manager {

    private static int changelog_requests = 0;
    private static int updater_id = 0;
    private static int alert_id = 0;

    public static void initialize() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
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
            PlayerFile.migrateV3();

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

            PluginConfiguration config = CurrentPlatform.getConfiguration();
            if (config.isBungeeCord()) {
                Messenger messenger = plugin.getServer().getMessenger();
                BungeeReceiver receiver = new BungeeReceiver();

                PluginMessageListenerRegistration registration_account = messenger.registerIncomingPluginChannel(plugin, "ll:account", receiver);
                messenger.registerOutgoingPluginChannel(plugin, "ll:account");
                PluginMessageListenerRegistration registration_plugin = messenger.registerIncomingPluginChannel(plugin, "ll:plugin", receiver);
                PluginMessageListenerRegistration access_plugin = messenger.registerIncomingPluginChannel(plugin, "ll:access", receiver);
                messenger.registerOutgoingPluginChannel(plugin, "ll:access");

                if (registration_account.isValid() && registration_plugin.isValid() && access_plugin.isValid()) {
                    Console.send(plugin, "Registered plugin message listeners", Level.OK);
                } else {
                    Console.send(plugin, "Something went wrong while trying to register message listeners, things may not work properly", Level.GRAVE);
                }
            }

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
            }

            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                LockLoginPlaceholder placeholder = new LockLoginPlaceholder();
                if (placeholder.register()) {
                    Console.send(plugin, "Hooked and loaded placeholder expansion", Level.OK);
                } else {
                    Console.send(plugin, "Couldn't hook placeholder expansion", Level.GRAVE);
                }
            }

            performVersionCheck();
            if (config.getUpdaterOptions().isEnabled()) {
                scheduleVersionCheck();
            }
            scheduleAlertSystem();

            Button.preCache();

            registerMetrics();
            initPlayers();

            CurrentPlatform.setPrefix(config.getModulePrefix());
        });
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

        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            LockLoginPlaceholder placeholder = new LockLoginPlaceholder();
            if (placeholder.isRegistered()) {
                if (placeholder.unregister()) {
                    Console.send(plugin, "Unhooked placeholder expansion", Level.OK);
                } else {
                    Console.send(plugin, "Couldn't un-hook placeholder expansion", Level.GRAVE);
                }
            }
        }

        System.out.println();
        artGen.print(ml.karmaconfigs.api.common.Console.Colors.RED_BRIGHT, "LockLogin", size, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, character);
        Console.send("&eversion:&6 {0}", versionID);
        Console.send(" ");
        Console.send("&e-----------------------");

        endPlayers();
    }

    /**
     * Register plugin commands
     */
    protected static void registerCommands() {
        Set<String> unregistered = new LinkedHashSet<>();
        Set<String> registered = new HashSet<>();

        PluginConfiguration config = CurrentPlatform.getConfiguration();

        for (Class<?> clazz : SystemCommand.manager.recognizedClasses()) {
            if (clazz.isAnnotationPresent(SystemCommand.class)) {
                try {
                    String command = SystemCommand.manager.getDeclaredCommand(clazz);
                    boolean bungee = SystemCommand.manager.getBungeeStatus(clazz);

                    if (command == null || command.replaceAll("\\s", "").isEmpty()) {
                        continue;
                    }

                    PluginCommand pluginCMD = plugin.getCommand(command);

                    if (pluginCMD == null) {
                        unregistered.add(command);
                        continue;
                    }

                    if (config.isBungeeCord() && !bungee) {
                        registered.add("/" + command);
                        for (String alias : pluginCMD.getAliases())
                            registered.add("/" + alias);
                        continue;
                    }

                    Object instance = clazz.getDeclaredConstructor().newInstance();

                    if (instance instanceof CommandExecutor) {
                        CommandExecutor executor = (CommandExecutor) instance;
                        pluginCMD.setExecutor(executor);

                        registered.add("/" + command);
                        for (String alias : pluginCMD.getAliases())
                            registered.add("/" + alias);
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (!unregistered.isEmpty()) {
            Console.send(plugin, properties.getProperty("command_register_problem", "Failed to register command(s): {0}"), Level.GRAVE, setToString(unregistered));
            Console.send(plugin, properties.getProperty("plugin_error_disabling", "Disabling plugin due an internal error"), Level.INFO);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
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
                plugin.getServer().getPluginManager().disablePlugin(plugin);
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
     * Register plugin metrics
     */
    protected static void registerMetrics() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        Metrics metrics = new Metrics(plugin, 6513);

        metrics.addCustomChart(new SimplePie("used_locale", () -> config.getLang().friendlyName(config.getLangName())));
        metrics.addCustomChart(new SimplePie("clear_chat", () -> String.valueOf(config.clearChat())
                .replace("true", "Clear chat")
                .replace("false", "Don't clear chat")));
        metrics.addCustomChart(new SimplePie("sessions_enabled", () -> String.valueOf(config.enableSessions())
                .replace("true", "Sessions enabled")
                .replace("false", "Sessions disabled")));
    }

    /**
     * Register the plugin listeners
     */
    protected static void registerListeners() {
        Listener onJoin = new JoinListener();
        Listener onQuit = new QuitListener();
        Listener onChat = new ChatListener();
        Listener other = new OtherListener();
        Listener inventory = new InventoryListener();
        Listener interact = new InteractListener();

        plugin.getServer().getPluginManager().registerEvents(onJoin, plugin);
        plugin.getServer().getPluginManager().registerEvents(onQuit, plugin);
        plugin.getServer().getPluginManager().registerEvents(onChat, plugin);
        plugin.getServer().getPluginManager().registerEvents(other, plugin);
        plugin.getServer().getPluginManager().registerEvents(inventory, plugin);
        plugin.getServer().getPluginManager().registerEvents(interact, plugin);
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

                Console.send(plugin, "LockLogin is outdated! Current version is {0} but latest is {1}", Level.INFO, versionID, checker.getLatestVersion());
                Console.send(checker.getChangelog());

                Message messages = new Message();
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    User user = new User(player);
                    if (player.hasPermission(PluginPermission.applyUpdates())) {
                        user.send(messages.prefix() + "&dNew LockLogin version available, current is " + versionID + ", but latest is " + checker.getLatestVersion());
                        user.send(messages.prefix() + "&dRun /locklogin changelog to view the list of changes");
                    }
                }

                if (VersionDownloader.downloadUpdates()) {
                    if (VersionDownloader.isDownloading()) {
                        Console.send(plugin, properties.getProperty("updater_downloaded", "Downloaded latest version plugin instance, to apply the updates run /locklogin applyUpdates"), Level.INFO);
                    } else {
                        VersionDownloader downloader = new VersionDownloader(versionID, config.getUpdaterOptions().getChannel());
                        downloader.download(
                                file -> {
                                    Console.send(plugin, properties.getProperty("updater_downloaded", "Downloaded latest version plugin instance, to apply the updates run /locklogin applyUpdates"), Level.INFO);

                                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                                        User user = new User(player);
                                        if (player.hasPermission(PluginPermission.applyUpdates())) {
                                            user.send(messages.prefix() + properties.getProperty("updater_downloaded", "Downloaded latest version plugin instance, to apply the updates run /locklogin applyUpdates"));
                                        }
                                    }
                                },
                                error -> {
                                    if (error != null) {
                                        logger.scheduleLog(Level.GRAVE, error);
                                        logger.scheduleLog(Level.INFO, "Failed to download latest LockLogin instance");
                                        Console.send(plugin, properties.getProperty("updater_download_fail", "Failed to download latest LockLogin update ( {0} )"), Level.INFO, error.fillInStackTrace());
                                    }
                                });
                    }
                } else {
                    Console.send(plugin, "LockLogin auto download is disabled, you must download latest LockLogin version from {0}", Level.GRAVE, checker.getDownloadURL());

                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        User user = new User(player);
                        if (player.hasPermission(PluginPermission.applyUpdates())) {
                            user.send(messages.prefix() + "&dFollow console instructions to update");
                        }
                    }
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

        AdvancedPluginTimer timer = new AdvancedPluginTimer(config.getUpdaterOptions().getInterval(), true).setAsync(true).addActionOnEnd(Manager::performVersionCheck);
        if (config.getUpdaterOptions().isEnabled())
            timer.start();

        updater_id = timer.getTimerId();

    }

    /**
     * Schedule the alert system
     */
    protected static void scheduleAlertSystem() {
        AdvancedPluginTimer timer = new AdvancedPluginTimer(30, true).setAsync(true).addActionOnEnd(() -> {
            AlertSystem system = new AlertSystem();
            system.checkAlerts();

            if (system.available())
                Console.send(system.getMessage());
        });
        timer.start();

        alert_id = timer.getTimerId();

    }

    /**
     * Initialize already connected players
     *
     * This is util after plugin updates or
     * plugin load using third-party loaders
     */
    protected static void initPlayers() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            Message messages = new Message();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                User user = new User(player);
                ClientSession session = user.getSession();
                InetSocketAddress ip = player.getAddress();

                if (ip != null) {
                    IpData data = new IpData(ip.getAddress());
                    int amount = data.getClonesAmount();

                    if (amount + 1 == config.accountsPerIP()) {
                        user.kick(StringUtils.toColor(messages.maxIP()));
                        return;
                    }
                    data.addClone();
                }

                if (!config.isBungeeCord()) {
                    Proxy proxy = new Proxy(ip);
                    if (proxy.isProxy()) {
                        user.kick(messages.ipProxyError());
                        return;
                    }

                    user.savePotionEffects();
                    user.applySessionEffects();

                    if (config.clearChat()) {
                        for (int i = 0; i < 150; i++)
                            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> player.sendMessage(""));
                    }

                    BarMessage bar = new BarMessage(player, messages.captcha(session.getCaptcha()));
                    if (!session.isCaptchaLogged())
                        bar.send(true);

                    SessionCheck check = new SessionCheck(player, target -> {
                        bar.setMessage("");
                        bar.stop();
                    }, target -> {
                        bar.setMessage("");
                        bar.stop();
                    });

                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, check);
                }

                if (player.getLocation().getBlock().getType().name().contains("PORTAL"))
                    user.setTempSpectator(true);

                if (config.hideNonLogged()) {
                    ClientVisor visor = new ClientVisor(player);
                    visor.vanish();
                    visor.checkVanish();
                }

                UserHookEvent event = new UserHookEvent(fromPlayer(player), null);
                JavaModuleManager.callEvent(event);
            }
        });
    }

    /**
     * Finalize connected players sessions
     *
     * This is util after plugin updates or
     * plugin unload using third-party loaders
     */
    protected static void endPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            InetSocketAddress ip = player.getAddress();
            User user = new User(player);

            if (user.isLockLoginUser()) {
                if (ip != null) {
                    IpData data = new IpData(ip.getAddress());
                    data.delClone();
                }

                Config config = new Config();
                if (!config.isBungeeCord()) {
                    SessionKeeper keeper = new SessionKeeper(fromPlayer(player));
                    keeper.store();
                }

                //Last location will be always saved since if the server
                //owner wants to enable it, it would be good to see
                //the player last location has been stored to avoid
                //location problems
                LastLocation last_loc = new LastLocation(player);
                last_loc.save();

                ClientSession session = user.getSession();
                session.invalidate();
                session.setLogged(false);
                session.setPinLogged(false);
                session.set2FALogged(false);

                user.removeLockLoginUser();
            }

            user.setTempSpectator(false);

            ClientVisor visor = new ClientVisor(player);
            visor.unVanish();
            visor.checkVanish();

            UserUnHookEvent event = new UserUnHookEvent(fromPlayer(player), null);
            JavaModuleManager.callEvent(event);
        }
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
