package eu.locklogin.plugin.bungee.plugin;

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
import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.common.communication.DataSender;
import eu.locklogin.api.common.premium.DefaultPremiumDatabase;
import eu.locklogin.api.common.security.client.ProxyCheck;
import eu.locklogin.api.common.session.Session;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.session.online.SessionDataContainer;
import eu.locklogin.api.common.session.persistence.SessionKeeper;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.filter.ConsoleFilter;
import eu.locklogin.api.common.utils.filter.PluginFilter;
import eu.locklogin.api.common.utils.other.ASCIIArtGenerator;
import eu.locklogin.api.common.utils.other.PlayerAccount;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.common.web.STFetcher;
import eu.locklogin.api.common.web.VersionDownloader;
import eu.locklogin.api.common.web.alert.Notification;
import eu.locklogin.api.common.web.alert.RemoteNotification;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.UserHookEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserUnHookEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.Main;
import eu.locklogin.plugin.bungee.com.BungeeDataSender;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.command.util.SystemCommand;
import eu.locklogin.plugin.bungee.listener.ChatListener;
import eu.locklogin.plugin.bungee.listener.JoinListener;
import eu.locklogin.plugin.bungee.listener.MessageListener;
import eu.locklogin.plugin.bungee.listener.QuitListener;
import eu.locklogin.plugin.bungee.plugin.injector.Injector;
import eu.locklogin.plugin.bungee.plugin.injector.ModuleExecutorInjector;
import eu.locklogin.plugin.bungee.util.files.Config;
import eu.locklogin.plugin.bungee.util.files.Message;
import eu.locklogin.plugin.bungee.util.files.Proxy;
import eu.locklogin.plugin.bungee.util.files.data.RestartCache;
import eu.locklogin.plugin.bungee.util.player.User;
import lombok.Getter;
import ml.karmaconfigs.api.common.data.file.FileUtilities;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.file.yaml.FileCopy;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.checker.VersionUpdater;
import ml.karmaconfigs.api.common.version.updater.VersionCheckType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static eu.locklogin.plugin.bungee.LockLogin.*;

public final class Manager {

    private static String last_notification_text = "";
    private static int last_notification_level = 0;

    /**
     * -- GETTER --
     *  Get the version updater
     */
    @Getter
    private static VersionUpdater updater = null;
    private static int changelog_requests = 0;
    private static int updater_id = 0;
    private static int alert_id = 0;

    /**
     * -- GETTER --
     *  Get if LockLogin has been initialized
     */
    @Getter
    private static boolean initialized = false;

    public static DataSender<ServerInfo> sender;

    private static Function<Void, Void> end;

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
        console.send("&eversion:&6 {0}", version);
        console.send("&eSpecial thanks: &7" + STFetcher.getDonors());

        ProxyCheck.scan();

        PlayerAccount.migrateV3();

        setupFiles();
        //TokenGen.generate(CurrentPlatform.getProxyConfiguration().proxyKey());
        registerCommands();
        registerListeners();

        console.send(" ");
        console.send("&e-----------------------");

        try {
            JarManager.changeField(CurrentPlatform.class, "default_manager", PlayerAccount.class);
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
        }
        if (!CurrentPlatform.isValidAccountManager()) {
            CurrentPlatform.setAccountsManager(PlayerAccount.class);
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

        PluginConfiguration config = CurrentPlatform.getConfiguration();

        plugin.getProxy().registerChannel(Channel.ACCOUNT.getName());
        plugin.getProxy().registerChannel(Channel.PLUGIN.getName());
        plugin.getProxy().registerChannel(Channel.ACCESS.getName());

        if (config.useVirtualID()) {
            CryptoFactory.loadVirtualID();
        } else {
            console.send("Virtual ID ( disabled by default) is disabled. You should enable it to enforce you clients security against database leaks", Level.GRAVE);
        }

        performVersionCheck();
        if (config.getUpdaterOptions().isEnabled()) {
            scheduleVersionCheck();
        }

        scheduleAlertSystem();

        CurrentPlatform.setPrefix(config.getModulePrefix());
        Injector injector = new ModuleExecutorInjector();

        injector.inject();

        console.send("Connecting to LockLogin web services (statistics and spigot communication)", Level.INFO);

        sender = new BungeeDataSender();

        plugin.async().queue("connect_web_services", () -> {
            initPlayers();
            registerMetrics();

            CurrentPlatform.setPremiumDatabase(new DefaultPremiumDatabase());
            initialized = true;

            AtomicInteger lastLogged = new AtomicInteger();
            AtomicInteger lastRegistered = new AtomicInteger();
            ProxyServer.getInstance().getScheduler().schedule(plugin, () -> plugin.async().queue("data_changer", () -> {
                int logged = SessionDataContainer.getLogged();
                int registered = SessionDataContainer.getRegistered();

                if (lastLogged.get() != logged || lastRegistered.get() != registered) {
                    Collection<ServerInfo> servers = plugin.getProxy().getServers().values();

                    for (ServerInfo server : servers) {
                        sender.queue(server)
                                .insert(DataMessage.newInstance(DataType.LOGGED, Channel.PLUGIN, server.getPlayers().stream().findAny().orElse(null))
                                        .addProperty("login_count", SessionDataContainer.getLogged()).getInstance().build());

                        sender.queue(server)
                                .insert(DataMessage.newInstance(DataType.REGISTERED, Channel.PLUGIN, server.getPlayers().stream().findAny().orElse(null))
                                        .addProperty("register_count", SessionDataContainer.getRegistered()).getInstance().build());
                    }

                    lastLogged.set(logged);
                    lastRegistered.set(registered);
                }
            }), 0, 5, TimeUnit.SECONDS);

            RemoteNotification notification = new RemoteNotification();
            notification.checkAlerts().whenComplete(() -> plugin.console().send(notification.getStartup()));
        });

        end = unused -> {
            endPlayers();
            return null;
        };
    }

    public static void terminate() {
        initialized = false;

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
        } catch (Throwable ignored) {}

        int size = 10;
        String character = "*";
        try {
            size = Integer.parseInt(properties.getProperty("ascii_art_size", "10"));
            character = properties.getProperty("ascii_art_character", "*").substring(0, 1);
        } catch (Throwable ignored) {
        }

        System.out.println();
        artGen.print("\u001B[31m", "LockLogin", size, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, character);
        console.send("&eversion:&6 {0}", version);
        console.send(" ");
        console.send("&e-----------------------");

        end.apply(null);
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

                        if (instance instanceof Command) {
                            Command executor = (Command) instance;
                            plugin.getProxy().getPluginManager().registerCommand(plugin, executor);
                            registered.add("/" + command.toLowerCase());
                        } else {
                            unregistered.add(command);
                        }
                    }
                } catch (Throwable ex) {
                    logger.scheduleLog(Level.GRAVE, ex);
                }
            }
        }

        if (!unregistered.isEmpty()) {
            console.send(properties.getProperty("command_register_problem", "Failed to register command(s): {0}"), Level.GRAVE, setToString(unregistered));
            console.send(properties.getProperty("plugin_error_disabling", "Disabling plugin due an internal error"), Level.INFO);
        } else {
            console.send(properties.getProperty("plugin_filter_initialize", "Initializing console filter to protect user data"), Level.INFO);

            try {
                Logger coreLogger = (Logger) LogManager.getRootLogger();
                ConsoleFilter filter = new ConsoleFilter(registered);

                coreLogger.addFilter(filter);
            } catch (Throwable ex) {
                console.send("LockLogin tried to hook into console filter, but as expected, BungeeCord or this BungeeCord fork doesn't has a valid logger, please do not report the commands are being shown in console", Level.GRAVE);
            }
        }
    }

    /**
     * Setup the plugin files
     */
    static void setupFiles() {
        Set<String> failed = new LinkedHashSet<>();

        File cfg = new File(plugin.getDataFolder(), "config.yml");
        File proxy = new File(plugin.getDataFolder(), "proxy.yml");

        FileCopy config_copy = new FileCopy(plugin, "cfg/config.yml");
        FileCopy proxy_copy = new FileCopy(plugin, "cfg/proxy.yml");
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
        File msg_file = new File(plugin.getDataFolder() + File.separator + "lang" + File.separator + "v2", "messages_" + country + ".yml");

        InputStream internal = Main.class.getResourceAsStream("/lang/messages_" + country + ".yml");
        if (internal != null) {
            FileCopy copy = new FileCopy(plugin, "lang/messages_" + country + ".yml");

            try {
                copy.copy(msg_file);
            } catch (Throwable ex) {
                failed.add(msg_file.getName());
            }
        } else {
            if (!msg_file.exists()) {
                failed.add(msg_file.getName());
                console.send("Could not find community message pack named {0} in lang_v2 folder, using messages english as default", Level.GRAVE, msg_file.getName());

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
        if (config.shareBStats()) {
            Metrics metrics = new Metrics(plugin, 6512);

            metrics.addCustomChart(new SimplePie("used_locale", () -> config.getLang().friendlyName(config.getLangName())));
            metrics.addCustomChart(new SimplePie("clear_chat", () -> String.valueOf(config.clearChat())
                    .replace("true", "Clear chat")
                    .replace("false", "Don't clear chat")));
            metrics.addCustomChart(new SimplePie("sessions_enabled", () -> String.valueOf(config.enableSessions())
                    .replace("true", "Sessions enabled")
                    .replace("false", "Sessions disabled")));
        } else {
            console.send("Metrics are disabled, please note this is an open source free project and we use metrics to know if the project is being active by users. If we don't see active users using this project, the project may reach the dead line meaning no more updates or support. We highly recommend to you to share statistics, as this won't share any information of your server but the country, os and some other information that may be util for us", Level.GRAVE);
        }
    }

    /**
     * Register the plugin listeners
     */
    static void registerListeners() {
        JoinListener onJoin = new JoinListener();
        Listener onQuit = new QuitListener(onJoin);
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
    static void loadCache() {
        RestartCache cache = new RestartCache();
        cache.loadUserData();

        cache.remove();
    }

    /**
     * Perform a version check
     */
    @SuppressWarnings("deprecation")
    static void performVersionCheck() {
        try {
            if (updater == null)
                updater = VersionUpdater.createNewBuilder(plugin).withVersionType(VersionCheckType.RESOLVABLE_ID).withVersionResolver(versionID).build();

            updater.fetch(true).whenComplete((fetch, trouble) -> {
                if (trouble == null) {
                    if (!fetch.isUpdated()) {
                        if (changelog_requests <= 0) {
                            changelog_requests = 3;

                            console.send("LockLogin is outdated! Current version is {0} but latest is {1}", Level.INFO, version, fetch.getLatest());
                            for (String line : fetch.getChangelog())
                                console.send(line);

                            PluginMessages messages = CurrentPlatform.getMessages();
                            for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                                User user = new User(player);
                                if (user.hasPermission(PluginPermissions.updater_apply())) {
                                    user.send(messages.prefix() + "&dNew LockLogin version available, current is " + version + ", but latest is " + fetch.getLatest());
                                    user.send(messages.prefix() + "&dRun /locklogin changelog to view the list of changes");
                                }
                            }

                            if (VersionDownloader.downloadUpdates()) {
                                if (VersionDownloader.canDownload()) {
                                    VersionDownloader.download();
                                }
                            } else {
                                console.send("LockLogin auto download is disabled, you must download latest LockLogin version from {0}", Level.GRAVE, fetch.getUpdateURL());

                                for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                                    User user = new User(player);
                                    if (user.hasPermission(PluginPermissions.updater_apply())) {
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
        } catch (IllegalStateException error) {
            console.send("Failed to setup plugin updater; {0}", Level.GRAVE, error.fillInStackTrace());
        }
    }

    /**
     * Schedule the version check process
     */
    static void scheduleVersionCheck() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        SimpleScheduler timer = new SourceScheduler(plugin, config.getUpdaterOptions().getInterval(), SchedulerUnit.SECOND, true).multiThreading(true).restartAction(Manager::performVersionCheck);
        if (config.getUpdaterOptions().isEnabled()) {
            timer.start();
        } else {
            performVersionCheck();
        }

        updater_id = timer.getId();

    }

    /**
     * Schedule the alert system
     */
    static void scheduleAlertSystem() {
        SimpleScheduler timer = new SourceScheduler(plugin, 30, SchedulerUnit.SECOND, true).multiThreading(true).restartAction(() -> {
            RemoteNotification system = new RemoteNotification();
            system.checkAlerts();

            Notification notification = system.getNotification();
            String text = notification.getNotification();
            int level = notification.getLevel();

            if (!last_notification_text.equals(text) || last_notification_level != level) {
                last_notification_text = text;
                last_notification_level = level;

                if (level == 0) {
                    console.send("( {0} ) " + text, Level.OK, level);
                } else {
                    if (level <= 4) {
                        console.send("( {0} ) " + text, Level.INFO, level);
                    } else {
                        if (level <= 7) {
                            console.send("( {0} ) " + text, Level.WARNING, level);
                        } else {
                            console.send("( {0} ) " + text, Level.GRAVE, level);
                        }
                    }
                }

                if (notification.forceConfig()) {
                    try {
                        JarManager.changeField(CurrentPlatform.class, "fake_config", system.getRemoteConfig());
                    } catch (Throwable ex) {
                        logger.scheduleLog(Level.GRAVE, ex);
                    }
                    console.send("The current alert system requires some configuration options to be in a specified value. Custom config will be ignored for some variables", Level.WARNING);
                }
                if (notification.forceProxy()) {
                    try {
                        JarManager.changeField(CurrentPlatform.class, "fake_proxy", system.getRemoteProxyConfig());
                    } catch (Throwable ex) {
                        logger.scheduleLog(Level.GRAVE, ex);
                    }
                    console.send("The current alert system requires some PROXY configuration options to be in a specified value. Custom PROXY config will be ignored for some variables", Level.WARNING);
                }
            }
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
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            PluginMessages messages = CurrentPlatform.getMessages();

            for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                plugin.getProxy().getScheduler().schedule(plugin, () -> {
                    InetSocketAddress ip = getSocketIp(player.getSocketAddress());
                    User user = new User(player);

                    Server server = player.getServer();
                    if (server != null) {
                        ServerInfo info = server.getInfo();
                        ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

                        if (ServerDataStorage.needsProxyKnowledge(info.getName())) {
                            sender.queue(info).insert(DataMessage.newInstance(DataType.REGISTER, Channel.ACCESS, player)
                                    .addProperty("key", proxy.proxyKey())
                                    .addProperty("server", info.getName())
                                    .addProperty("socket", false).getInstance().build(), true);
                        }
                    }

                    CurrentPlatform.requestDataContainerUpdate();

                    sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.VALIDATION, Channel.ACCOUNT, player)
                            .getInstance().build());

                    ProxyCheck proxy = new ProxyCheck(ip);
                    if (proxy.isProxy()) {
                        user.kick(messages.ipProxyError());
                        return;
                    }

                    user.applySessionEffects();

                    if (config.clearChat()) {
                        for (int i = 0; i < 150; i++)
                            plugin.getProxy().getScheduler().runAsync(plugin, () -> player.sendMessage(TextComponent.fromLegacyText("")));
                    }

                    ClientSession session = user.getSession();
                    AccountManager manager = user.getManager();
                    session.validate();

                    if (!config.captchaOptions().isEnabled())
                        session.setCaptchaLogged(true);

                    SimpleScheduler tmp_timer = null;
                    if (config.captchaOptions().isEnabled()) {
                        if (!session.isCaptchaLogged()) {
                            tmp_timer = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true);
                            tmp_timer.changeAction((second) -> player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(StringUtils.toColor(messages.captcha(session.getCaptcha()))))).start();
                        }
                    }

                    sender.queue(BungeeSender.serverFromPlayer(player)).insert(
                            DataMessage.newInstance(DataType.JOIN, Channel.ACCOUNT, player)
                                    .addProperty("pass_login", session.isLogged())
                                    .addProperty("2fa_login", session.is2FALogged())
                                    .addProperty("pin_login", session.isPinLogged())
                                    .addProperty("registered", manager.isRegistered())
                                    .getInstance().build()
                    );

                    SimpleScheduler timer = tmp_timer;
                    SessionCheck<ProxiedPlayer> check = user.getChecker().whenComplete(() -> {
                        user.restorePotionEffects();
                        player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));

                        if (timer != null)
                            timer.cancel();
                    });

                    plugin.getProxy().getScheduler().runAsync(plugin, check);

                    user.checkServer(0, false);

                    Event event = new UserHookEvent(user.getModule(), null);
                    ModulePlugin.callEvent(event);
                }, 2, TimeUnit.SECONDS);
            }
        });
    }

    /**
     * Finalize connected players sessions
     * <p>
     * This is util after plugin updates or
     * plugin unload using third-party loaders
     */
    static void endPlayers() {
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            User user = new User(player);

            SessionKeeper keeper = new SessionKeeper(user.getModule());
            keeper.store();

            ClientSession session = user.getSession();
            session.invalidate();
            session.setLogged(false);
            session.setPinLogged(false);
            session.set2FALogged(false);

            sender.queue(BungeeSender.serverFromPlayer(player))
                    .insert(DataMessage.newInstance(DataType.QUIT, Channel.ACCOUNT, player)
                            .getInstance().build());

            Event event = new UserUnHookEvent(user.getModule(), null);
            ModulePlugin.callEvent(event);
        }
    }

    /**
     * Restart the version checker
     */
    public static void restartVersionChecker() {
        try {
            SimpleScheduler timer = new SourceScheduler(plugin, updater_id);
            timer.restart();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Restart the alert system timer
     */
    public static void restartAlertSystem() {
        try {
            SimpleScheduler timer = new SourceScheduler(plugin, alert_id);
            timer.restart();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Connect to the remote locklogin messaging server
     *
     * @param address the remote server address
     * @param port the remote server port
     * @deprecated Implemented in another better way
     */
    @Deprecated @ApiStatus.ScheduledForRemoval
    @SuppressWarnings("all")
    public static CompletableFuture<Boolean> connect(final String address, final int port) {
        /*
        Actually this is ready to work. We should only have to make the API For it lol

        Factory factory = new Factory(WorkLevel.TCP);
        client = factory.createClient(address, port);

        return client.connect();*/

        return null;
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
