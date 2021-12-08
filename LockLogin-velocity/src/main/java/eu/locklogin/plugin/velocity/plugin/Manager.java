package eu.locklogin.plugin.velocity.plugin;

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

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.TokenGen;
import eu.locklogin.api.common.security.client.ProxyCheck;
import eu.locklogin.api.common.session.Session;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.common.session.SessionKeeper;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.filter.ConsoleFilter;
import eu.locklogin.api.common.utils.filter.PluginFilter;
import eu.locklogin.api.common.utils.other.ASCIIArtGenerator;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.common.web.AlertSystem;
import eu.locklogin.api.common.web.STFetcher;
import eu.locklogin.api.common.web.VersionDownloader;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.UserHookEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserUnHookEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.velocity.Main;
import eu.locklogin.plugin.velocity.command.util.BungeeLikeCommand;
import eu.locklogin.plugin.velocity.command.util.SystemCommand;
import eu.locklogin.plugin.velocity.listener.ChatListener;
import eu.locklogin.plugin.velocity.listener.JoinListener;
import eu.locklogin.plugin.velocity.listener.MessageListener;
import eu.locklogin.plugin.velocity.listener.QuitListener;
import eu.locklogin.plugin.velocity.permissibles.PluginPermission;
import eu.locklogin.plugin.velocity.plugin.injector.Injector;
import eu.locklogin.plugin.velocity.plugin.injector.VelocityInjector;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.files.Config;
import eu.locklogin.plugin.velocity.util.files.Message;
import eu.locklogin.plugin.velocity.util.files.Proxy;
import eu.locklogin.plugin.velocity.util.files.client.PlayerFile;
import eu.locklogin.plugin.velocity.util.files.data.RestartCache;
import eu.locklogin.plugin.velocity.util.files.data.lock.LockedAccount;
import eu.locklogin.plugin.velocity.util.player.PlayerPool;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.karmafile.karmayaml.FileCopy;
import ml.karmaconfigs.api.common.timer.SourceSecondsTimer;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.common.version.VersionUpdater;
import ml.karmaconfigs.api.common.version.util.VersionCheckType;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static eu.locklogin.plugin.velocity.LockLogin.*;

public final class Manager {

    private static VersionUpdater updater = null;
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
        artGen.print("\u001B[33m", "LockLogin", size, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, character);
        console.send("&eversion:&6 {0}", versionID.getVersionID());
        console.send("&eSpecial thanks: &7" + STFetcher.getDonors());

        ProxyCheck.scan();

        PlayerFile.migrateV1();
        PlayerFile.migrateV2();
        PlayerFile.migrateV3();
        PlayerPool.startCheckTask();

        setupFiles();
        TokenGen.generate(CurrentPlatform.getProxyConfiguration().proxyKey());
        registerCommands();
        registerListeners();

        console.send(" ");
        console.send("&e-----------------------");

        if (!CurrentPlatform.isValidAccountManager()) {
            CurrentPlatform.setAccountsManager(PlayerFile.class);
            console.send("Loaded native player account manager", Level.INFO);
        } else {
            console.send("Loaded custom player account manager", Level.INFO);
        }
        if (!CurrentPlatform.isValidSessionManager()) {
            CurrentPlatform.setSessionManager(Session.class);
            console.send("Loaded native player session manager", Level.INFO);
        } else {
            console.send("Loaded custom player session manager", Level.INFO);
        }

        loadCache();

        server.getChannelRegistrar().register(new LegacyChannelIdentifier(DataSender.CHANNEL_PLAYER));
        server.getChannelRegistrar().register(new LegacyChannelIdentifier(DataSender.PLUGIN_CHANNEL));
        server.getChannelRegistrar().register(new LegacyChannelIdentifier(DataSender.ACCESS_CHANNEL));

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
                    Collection<RegisteredServer> servers = server.getAllServers();

                    switch (data.getDataType()) {
                        case LOGIN:
                            for (RegisteredServer server : servers) {
                                DataSender.send(server, DataSender.getBuilder(DataType.LOGGED, DataSender.PLUGIN_CHANNEL, null).addIntData(SessionDataContainer.getLogged()).build());
                            }
                            break;
                        case REGISTER:
                            for (RegisteredServer server : servers) {
                                DataSender.send(server, DataSender.getBuilder(DataType.REGISTERED, DataSender.PLUGIN_CHANNEL, null).addIntData(SessionDataContainer.getRegistered()).build());
                            }
                            break;
                        default:
                            break;
                    }
                } catch (Throwable ignored) {
                }
            });
        }

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        performVersionCheck();
        if (config.getUpdaterOptions().isEnabled()) {
            scheduleVersionCheck();
        }
        scheduleAlertSystem();

        registerMetrics();
        initPlayers();

        CurrentPlatform.setPrefix(config.getModulePrefix());

        Injector injector = new VelocityInjector();
        injector.inject();
    }

    public static void terminate() {
        try {
            console.send("Finalizing console filter, please wait", Level.INFO);
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
        artGen.print("\u001B[31m", "LockLogin", size, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, character);
        console.send("&eversion:&6 {0}", versionID.getVersionID());
        console.send(" ");
        console.send("&e-----------------------");

        endPlayers();
    }

    /**
     * Register plugin commands
     */
    static void registerCommands() {
        Set<String> unregistered = new LinkedHashSet<>();
        Set<String> registered = new HashSet<>();

        for (Class<?> clazz : SystemCommand.manager.recognizedClasses()) {
            if (clazz.isAnnotationPresent(SystemCommand.class)) {
                try {
                    String command = SystemCommand.manager.getDeclaredCommand(clazz);
                    List<String> aliases = SystemCommand.manager.getDeclaredAliases(clazz);

                    if (command != null && !command.replaceAll("\\s", "").isEmpty()) {
                        Object instance = clazz.getDeclaredConstructor(String.class, List.class).newInstance(command, aliases);

                        if (instance instanceof BungeeLikeCommand) {
                            BungeeLikeCommand executor = (BungeeLikeCommand) instance;
                            server.getCommandManager().register(command, (SimpleCommand) invocation -> executor.execute(invocation.source(), invocation.arguments()), executor.getAliasses());
                            registered.add("/" + command);
                        } else {
                            unregistered.add(command);
                        }
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (!unregistered.isEmpty()) {
            console.send(properties.getProperty("command_register_problem", "Failed to register command(s): {0}"), Level.GRAVE, setToString(unregistered));
            console.send(properties.getProperty("plugin_error_disabling", "Disabling plugin due an internal error"), Level.INFO);
        } else {
            console.send(properties.getProperty("plugin_filter_initialize", "Initializing console filter to protect user data"), Level.INFO);

            try {
                ConsoleFilter filter = new ConsoleFilter(registered);

                Logger coreLogger = (Logger) LogManager.getRootLogger();
                coreLogger.addFilter(filter);
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
                logger.scheduleLog(Level.INFO, "Failed to register console filter");

                console.send(properties.getProperty("plugin_filter_error", "An error occurred while initializing console filter, check logs for more info"), Level.GRAVE);
                console.send(properties.getProperty("plugin_error_disabling", "Disabling plugin due an internal error"), Level.INFO);
            }
        }
    }

    /**
     * Setup the plugin files
     */
    static void setupFiles() {
        Set<String> failed = new LinkedHashSet<>();

        File cfg = new File(source.getDataPath().toFile(), "config.yml");
        File proxy = new File(source.getDataPath().toFile(), "proxy.yml");

        FileCopy config_copy = new FileCopy(source, "cfg/config.yml");
        FileCopy proxy_copy = new FileCopy(source, "cfg/proxy.yml");
        try {
            config_copy.copy(cfg);
        } catch (Throwable ex) {
            failed.add("config.yml");
        }
        try {
            proxy_copy.copy(proxy);
        } catch (Throwable ex) {
            failed.add("proxy.yml");
        }

        Config config = new Config();
        Proxy proxy_cfg = new Proxy();
        CurrentPlatform.setConfigManager(config);
        CurrentPlatform.setProxyManager(proxy_cfg);

        String country = config.getLang().country(config.getLangName());
        File msg_file = new File(source.getDataPath().toFile() + File.separator + "lang" + File.separator + "v2", "messages_" + country + ".yml");

        InputStream internal = Main.class.getResourceAsStream("/lang/messages_" + country + ".yml");
        //Check if the file exists inside the plugin as an official language
        if (internal != null) {
            FileCopy copy = new FileCopy(source, "lang/messages_" + country + ".yml");

            try {
                copy.copy(msg_file);
            } catch (Throwable ex) {
                failed.add(msg_file.getName());
            }
        } else {
            if (!msg_file.exists()) {
                failed.add(msg_file.getName());
                console.send("Could not find community message pack named {0} in lang_v2 folder, using messages english as default", Level.GRAVE, msg_file.getName());

                msg_file = new File(source.getDataPath().toFile() + File.separator + "lang" + File.separator + "v2", "messages_en.yml");

                if (!msg_file.exists()) {
                    FileCopy copy = new FileCopy(source, "lang/messages_en.yml");

                    try {
                        copy.copy(msg_file);
                    } catch (Throwable ex) {
                        failed.add(msg_file.getName());
                    }
                }
            } else {
                console.send("Detected community language pack, please make sure this pack is updated to avoid translation errors", Level.WARNING);
            }
        }

        if (!failed.isEmpty()) {
            console.send(properties.getProperty("file_register_problem", "Failed to setup/check file(s): {0}. The plugin will use defaults, you can try to create files later by running /locklogin reload"), Level.WARNING, setToString(failed));
        }

        Message messages = new Message();

        Config.manager.checkValues();
        CurrentPlatform.setPluginMessages(messages);
    }

    /**
     * Register plugin metrics
     */
    static void registerMetrics() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        Metrics metrics = factory.make(main, 11291);

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
    static void registerListeners() {
        JoinListener onJoin = new JoinListener();
        QuitListener onQuit = new QuitListener();
        ChatListener onChat = new ChatListener();
        MessageListener onMessage = new MessageListener();

        server.getEventManager().register(plugin, onJoin);
        server.getEventManager().register(plugin, onQuit);
        server.getEventManager().register(plugin, onChat);
        server.getEventManager().register(plugin, onMessage);
    }

    /**
     * Load the plugin cache if exists
     */
    static void loadCache() {
        RestartCache cache = new RestartCache();
        cache.loadUserData();

        cache.remove();
    }

    /**
     * Perform a version check
     */
    static void performVersionCheck() {
        if (updater == null)
            updater = VersionUpdater.createNewBuilder(source).withVersionType(VersionCheckType.RESOLVABLE_ID).withVersionResolver(versionID).build();

        updater.fetch(true).whenComplete((fetch, trouble) -> {
            if (trouble == null) {
                if (!fetch.isUpdated()) {
                    if (changelog_requests <= 0) {
                        changelog_requests = 3;

                        console.send("LockLogin is outdated! Current version is {0} but latest is {1}", Level.INFO, versionID.getVersionID(), fetch.getLatest());
                        for (String line : fetch.getChangelog())
                            console.send(line);

                        PluginMessages messages = CurrentPlatform.getMessages();
                        for (Player player : server.getAllPlayers()) {
                            User user = new User(player);
                            if (user.hasPermission(PluginPermission.applyUpdates())) {
                                user.send(messages.prefix() + "&dNew LockLogin version available, current is " + versionID.getVersionID() + ", but latest is " + fetch.getLatest());
                                user.send(messages.prefix() + "&dRun /locklogin changelog to view the list of changes");
                            }
                        }

                        if (VersionDownloader.downloadUpdates()) {
                            if (VersionDownloader.canDownload()) {
                                VersionDownloader downloader = new VersionDownloader(fetch);
                                downloader.download().whenComplete((file, error) -> {
                                    if (error != null) {
                                        logger.scheduleLog(Level.GRAVE, error);
                                        logger.scheduleLog(Level.INFO, "Failed to download latest LockLogin instance");
                                        console.send(properties.getProperty("updater_download_fail", "Failed to download latest LockLogin update ( {0} )"), Level.INFO, error.fillInStackTrace());

                                        try {
                                            Files.deleteIfExists(file.toPath());
                                        } catch (Throwable ignored) {
                                        }
                                    } else {
                                        console.send(properties.getProperty("updater_downloaded", "Downloaded latest version plugin instance, to apply the updates run /locklogin applyUpdates"), Level.INFO);

                                        for (Player player : server.getAllPlayers()) {
                                            User user = new User(player);
                                            if (user.hasPermission(PluginPermission.applyUpdates())) {
                                                user.send(messages.prefix() + properties.getProperty("updater_downloaded", "Downloaded latest version plugin instance, to apply the updates run /locklogin applyUpdates"));
                                            }
                                        }
                                    }
                                });
                            }
                        } else {
                            console.send("LockLogin auto download is disabled, you must download latest LockLogin version from {0}", Level.GRAVE, fetch.getUpdateURL());

                            for (Player player : server.getAllPlayers()) {
                                User user = new User(player);
                                if (user.hasPermission(PluginPermission.applyUpdates())) {
                                    user.send(messages.prefix() + "&dFollow console instructions to update");
                                }
                            }
                        }
                    } else {
                        changelog_requests--;
                    }
                }
            } else {
                logger.scheduleLog(Level.GRAVE, trouble);
                logger.scheduleLog(Level.INFO, "Failed to check for updates");
            }
        });
    }

    /**
     * Schedule the version check process
     */
    static void scheduleVersionCheck() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        SimpleScheduler timer = new SourceSecondsTimer(main, config.getUpdaterOptions().getInterval(), true).multiThreading(true).endAction(Manager::performVersionCheck);
        if (config.getUpdaterOptions().isEnabled())
            timer.start();

        updater_id = timer.getId();

    }

    /**
     * Schedule the alert system
     */
    static void scheduleAlertSystem() {
        SimpleScheduler timer = new SourceSecondsTimer(main, 30, true).multiThreading(true).endAction(() -> {
            AlertSystem system = new AlertSystem();
            system.checkAlerts();

            if (system.available())
                console.send(system.getMessage());
        });
        timer.start();

        alert_id = timer.getId();

    }

    /**
     * Initialize already connected players
     * <p>
     * This is util after plugin updates or
     * plugin load using third-party loaders
     */
    static void initPlayers() {
        server.getScheduler().buildTask(plugin, () -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            PluginMessages messages = CurrentPlatform.getMessages();

            for (Player player : server.getAllPlayers()) {
                server.getScheduler().buildTask(plugin, () -> {
                    InetSocketAddress ip = player.getRemoteAddress();
                    User user = new User(player);

                    Optional<ServerConnection> tmp_server = player.getCurrentServer();
                    if (tmp_server.isPresent()) {
                        ServerConnection connection = tmp_server.get();

                        RegisteredServer info = connection.getServer();
                        ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

                        if (ServerDataStorage.needsRegister(info.getServerInfo().getName()) || ServerDataStorage.needsProxyKnowledge(info.getServerInfo().getName())) {
                            if (ServerDataStorage.needsRegister(info.getServerInfo().getName()))
                                DataSender.send(info, DataSender.getBuilder(DataType.KEY, DataSender.ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getServerInfo().getName()).addBoolData(proxy.multiBungee()).build());

                            if (ServerDataStorage.needsProxyKnowledge(info.getServerInfo().getName())) {
                                DataSender.send(info, DataSender.getBuilder(DataType.REGISTER, DataSender.ACCESS_CHANNEL, player)
                                        .addTextData(proxy.proxyKey()).addTextData(info.getServerInfo().getName())
                                        .addTextData(TokenGen.expiration("LOCAL_TOKEN").toString())
                                        .build());
                            }
                        }
                    }

                    DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, DataSender.PLUGIN_CHANNEL, player).addTextData(CurrentPlatform.getConfiguration().toString()).build());
                    DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, DataSender.PLUGIN_CHANNEL, player).addTextData(Config.manager.getConfiguration()).build());
                    CurrentPlatform.requestDataContainerUpdate();

                    DataSender.MessageData validation = DataSender.getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER, player).build();
                    DataSender.send(player, validation);

                    ProxyCheck proxy = new ProxyCheck(ip);
                    if (proxy.isProxy()) {
                        user.kick(messages.ipProxyError());
                        return;
                    }

                    user.applySessionEffects();

                    if (config.clearChat()) {
                        for (int i = 0; i < 150; i++)
                            server.getScheduler().buildTask(plugin, () -> player.sendMessage(Component.text().content("").build()));
                    }

                    ClientSession session = user.getSession();
                    session.validate();

                    if (!config.captchaOptions().isEnabled())
                        session.setCaptchaLogged(true);

                    SimpleScheduler tmp_timer = null;
                    if (!session.isCaptchaLogged()) {
                        tmp_timer = new SourceSecondsTimer(main, 1, true);
                        tmp_timer.secondChangeAction((second) -> player.sendActionBar(Component.text().content(StringUtils.toColor(messages.captcha(session.getCaptcha()))).build())).start();
                    }

                    DataSender.MessageData join = DataSender.getBuilder(DataType.JOIN, DataSender.CHANNEL_PLAYER, player)
                            .addBoolData(session.isLogged())
                            .addBoolData(session.is2FALogged())
                            .addBoolData(session.isPinLogged())
                            .addBoolData(user.isRegistered()).build();
                    DataSender.send(player, join);

                    SimpleScheduler timer = tmp_timer;
                    SessionCheck<Player> check = user.getChecker().whenComplete(() -> {
                        user.removeSessionCheck();
                        player.sendActionBar(Component.text().content("").build());
                        if (timer != null)
                            timer.cancel();
                    });

                    server.getScheduler().buildTask(plugin, check).schedule();

                    user.checkServer(0);

                    Event event = new UserHookEvent(user.getModule(), null);
                    ModulePlugin.callEvent(event);
                }).delay(2, TimeUnit.SECONDS).schedule();
            }
        }).schedule();
    }

    /**
     * Finalize connected players sessions
     * <p>
     * This is util after plugin updates or
     * plugin unload using third-party loaders
     */
    static void endPlayers() {
        for (Player player : server.getAllPlayers()) {
            User user = new User(player);

            SessionKeeper keeper = new SessionKeeper(user.getModule());
            keeper.store();

            ClientSession session = user.getSession();
            session.invalidate();
            session.setLogged(false);
            session.setPinLogged(false);
            session.set2FALogged(false);

            DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());

            Event event = new UserUnHookEvent(user.getModule(), null);
            ModulePlugin.callEvent(event);
        }
    }

    /**
     * Restart the version checker
     */
    public static void restartVersionChecker() {
        try {
            SimpleScheduler timer = new SourceSecondsTimer(source, updater_id);
            timer.restart();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Restart the alert system timer
     */
    public static void restartAlertSystem() {
        try {
            SimpleScheduler timer = new SourceSecondsTimer(source, alert_id);
            timer.restart();
        } catch (Throwable ignored) {
        }
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

    /**
     * Get the version updater
     *
     * @return the version updater
     */
    public static VersionUpdater getUpdater() {
        return updater;
    }
}
