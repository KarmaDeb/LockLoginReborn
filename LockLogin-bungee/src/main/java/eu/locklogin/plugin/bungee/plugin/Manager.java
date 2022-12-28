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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.account.MigrationManager;
import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.common.security.client.ProxyCheck;
import eu.locklogin.api.common.session.Session;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.common.session.SessionKeeper;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.filter.ConsoleFilter;
import eu.locklogin.api.common.utils.filter.PluginFilter;
import eu.locklogin.api.common.utils.other.ASCIIArtGenerator;
import eu.locklogin.api.common.utils.other.LockedAccount;
import eu.locklogin.api.common.utils.other.PlayerAccount;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.common.web.STFetcher;
import eu.locklogin.api.common.web.VersionDownloader;
import eu.locklogin.api.common.web.alert.Notification;
import eu.locklogin.api.common.web.alert.RemoteNotification;
import eu.locklogin.api.common.web.services.LockLoginSocket;
import eu.locklogin.api.common.web.services.socket.SocketClient;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserHookEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPostValidationEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserUnHookEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bungee.Main;
import eu.locklogin.plugin.bungee.com.BungeeDataSender;
import eu.locklogin.plugin.bungee.com.ProxyDataSender;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.command.util.SystemCommand;
import eu.locklogin.plugin.bungee.listener.ChatListener;
import eu.locklogin.plugin.bungee.listener.JoinListener;
import eu.locklogin.plugin.bungee.listener.MessageListener;
import eu.locklogin.plugin.bungee.listener.QuitListener;
import eu.locklogin.plugin.bungee.plugin.injector.Injector;
import eu.locklogin.plugin.bungee.plugin.injector.ModuleExecutorInjector;
import eu.locklogin.plugin.bungee.plugin.socket.ConnectionManager;
import eu.locklogin.plugin.bungee.util.files.Config;
import eu.locklogin.plugin.bungee.util.files.Message;
import eu.locklogin.plugin.bungee.util.files.Proxy;
import eu.locklogin.plugin.bungee.util.files.data.RestartCache;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.karmafile.karmayaml.FileCopy;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.security.token.TokenGenerator;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.common.utils.url.HttpUtil;
import ml.karmaconfigs.api.common.utils.url.URLUtils;
import ml.karmaconfigs.api.common.version.VersionUpdater;
import ml.karmaconfigs.api.common.version.util.VersionCheckType;
import net.md_5.bungee.api.ChatMessageType;
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
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static eu.locklogin.plugin.bungee.LockLogin.*;

public final class Manager {

    private static String last_notification_text = "";
    private static int last_notification_level = 0;

    private static VersionUpdater updater = null;
    private static int changelog_requests = 0;
    private static int updater_id = 0;
    private static int alert_id = 0;

    private static boolean initialized = false;

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

        PlayerAccount.migrateV1();
        PlayerAccount.migrateV2();
        PlayerAccount.migrateV3();

        setupFiles();
        //TokenGen.generate(CurrentPlatform.getProxyConfiguration().proxyKey());
        registerCommands();
        registerListeners();

        console.send(" ");
        console.send("&e-----------------------");

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

        plugin.getProxy().registerChannel(Channel.ACCOUNT.getName());
        plugin.getProxy().registerChannel(Channel.PLUGIN.getName());
        plugin.getProxy().registerChannel(Channel.ACCESS.getName());

        AccountManager acc_manager = CurrentPlatform.getAccountManager(eu.locklogin.api.util.enums.Manager.CUSTOM, null);
        if (acc_manager != null) {
            Set<AccountManager> accounts = acc_manager.getAccounts();
            Set<AccountManager> nonLocked = new HashSet<>();
            for (AccountManager account : accounts) {
                LockedAccount locked = new LockedAccount(account.getUUID());
                if (!locked.isLocked())
                    nonLocked.add(account);
            }

            SessionDataContainer.setRegistered(nonLocked.size());

            SessionDataContainer.onDataChange(data -> {
                try {
                    Collection<ServerInfo> servers = plugin.getProxy().getServers().values();

                    switch (data.getDataType()) {
                        case LOGIN:
                            for (ServerInfo server : servers) {
                                BungeeSender.sender.queue(server.getName())
                                        .insert(DataMessage.newInstance(DataType.LOGGED, Channel.PLUGIN)
                                                .addProperty("login_count", SessionDataContainer.getLogged()).getInstance().build());
                            }
                            break;
                        case REGISTER:
                            for (ServerInfo server : servers) {
                                BungeeSender.sender.queue(server.getName())
                                        .insert(DataMessage.newInstance(DataType.REGISTERED, Channel.PLUGIN)
                                                .addProperty("register_count", SessionDataContainer.getRegistered()).getInstance().build());
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
        ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

        if (config.useVirtualID()) {
            CryptoFactory.loadVirtualID();
        } else {
            console.send("Virtual ID ( disabled by default) is disabled. You should enable it to enforce you clients security against database leaks", Level.GRAVE);
        }

        performVersionCheck();
        if (config.getUpdaterOptions().isEnabled()) {
            scheduleVersionCheck();
        }
        registerMetrics();

        scheduleAlertSystem();
        initPlayers();

        CurrentPlatform.setPrefix(config.getModulePrefix());
        Injector injector = new ModuleExecutorInjector();
        //As off 1.13.24 injector is the same for everyone! [ NO MORE REFLECTION LOL ]

        injector.inject();

        new SourceScheduler(plugin, 10, SchedulerUnit.SECOND, false).multiThreading(true)
                .endAction(() -> {
                    RemoteNotification notification = new RemoteNotification();
                    notification.checkAlerts().whenComplete(() -> console.send(notification.getStartup()));
                }).start();

        String[] tries = new String[]{
                "https://backup.karmadev.es/locklogin/com/",
                "https://backup.karmaconfigs.ml/locklogin/com/",
                "https://backup.karmarepo.ml/locklogin/com/",
                "https://karmadev.es/locklogin/com/",
                "https://karmaconfigs.ml/com/",
                "https://karmarepo.ml/com/"
        };

        console.send("Generating communication key, please wait...", Level.INFO);
        String token = TokenGenerator.generateLiteral(64);
        URL working = URLUtils.getOrBackup(tries);
        HttpUtil utilities = URLUtils.extraUtils(working);
        if (utilities != null) {
            String resul = utilities.getResponse();

            Gson gson = new GsonBuilder().create();
            JsonObject element = gson.fromJson(resul, JsonObject.class);
            try {
                boolean success = element.getAsJsonPrimitive("success").getAsBoolean();
                if (success) {
                    console.send("Loaded communication key from server", Level.INFO);
                    token = element.getAsJsonPrimitive("message").getAsString();
                } else {
                    console.send("Failed to generate communication key ({0}), a temporal one will be used", Level.WARNING, element.getAsJsonPrimitive("message").getAsString());
                }
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
                logger.scheduleLog(Level.INFO, "Failed to generate communication key");
                console.send("Failed to generate communication key (error), a temporal one will be used", Level.WARNING);
            }
        } else {
            console.send("Failed to generate communication key, a temporal one will be used", Level.WARNING);
        }

        try {
            Class<?> clazz = BungeeDataSender.class;
            Field com = clazz.getDeclaredField("com");
            Field sec = clazz.getDeclaredField("secret");
            com.setAccessible(true);
            sec.setAccessible(true);
            com.set(BungeeDataSender.class, token);
            sec.set(BungeeDataSender.class, proxy.getProxyID().toString());
            com.setAccessible(false);
            sec.setAccessible(false);

            console.send("Successfully defined communication key", Level.INFO);
        } catch (Throwable ignored) {}

        console.send("Connecting to LockLogin web services (statistics and spigot communication)", Level.INFO);
        SocketClient socket = new LockLoginSocket();
        ProxyDataSender pds = new ProxyDataSender(socket);
        ConnectionManager c_Manager = new ConnectionManager(socket, pds);

        //TODO: Launch web services communication project. For now this will never connect
        /*c_Manager.connect(5).whenComplete((tries_amount) -> {
            if (tries_amount > 0) {
                console.send("Connected to LockLogin web services after {0} tries", Level.WARNING, tries_amount);
            }

            switch (tries_amount) {
                case -1:
                    //TODO: Allow configure tries amount from config and read that value
                    console.send("Failed to connect to LockLogin web services after 5 tries, giving up...", Level.WARNING);
                case -2:
                    BungeeSender.sender = new BungeeDataSender();
                    BungeeSender.useSocket = false;
                    break;
                case 0:
                    console.send("Connected to LockLogin web services successfully", Level.INFO);
                default:
                    //JsonObject data = new JsonObject();
                    BungeeSender.sender = pds;
                    BungeeSender.useSocket = true;

                    c_Manager.onServerConnected((name, aka) -> {
                        ServerInfo server = plugin.getProxy().getServerInfo(name);
                        if (server != null) {
                            pds.addMap(server, aka);
                            console.send("Server {0} has been connected to the proxy", Level.INFO, name);
                        }
                    });
                    c_Manager.onProxyConnected((address) -> {
                        try {
                            InetAddress inet = InetAddress.getByName(address);
                            if (inet.isReachable(15000)) {
                                console.send("Connected proxy from {0}. Now sharing data with it", Level.INFO, address);
                            } else {
                                console.send("A proxy from {0} has connected the server, but we were unable to verify it's online. In a future, the connection will reset if that happens", Level.GRAVE, address);
                            }
                        } catch (Throwable ex) {
                            console.send("A proxy from {0} has connected the server, but we were unable to resolve its host. In a future the connection will reset if that happens", Level.GRAVE, address);
                        }
                    });

                    console.send("Registering LockLogin web service listeners", Level.INFO);
                    c_Manager.addListener(Channel.ACCOUNT, DataType.PIN, (server, data) -> {
                        if (data.has("pin_input") && data.has("player")) {
                            UUID uuid = UUID.fromString(data.get("player").getAsString());
                            ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

                            if (player != null) {
                                String pin = data.get("pin_input").getAsString();

                                User user = new User(player);
                                ClientSession session = user.getSession();
                                AccountManager manager = user.getManager();
                                if (session.isValid()) {
                                    PluginMessages messages = CurrentPlatform.getMessages();

                                    if (manager.hasPin() && CryptoFactory.getBuilder().withPassword(pin).withToken(manager.getPin()).build().validate(Validation.ALL) && !pin.equalsIgnoreCase("error")) {
                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                                (manager.has2FA() ? UserAuthenticateEvent.Result.SUCCESS_TEMP : UserAuthenticateEvent.Result.SUCCESS),
                                                user.getModule(),
                                                (manager.has2FA() ? messages.gAuthInstructions() : messages.logged()), null);
                                        ModulePlugin.callEvent(event);

                                        user.send(messages.prefix() + event.getAuthMessage());
                                        session.setPinLogged(true);
                                        if (manager.has2FA()) {
                                            session.set2FALogged(false);
                                        } else {
                                            session.set2FALogged(true);
                                            BungeeSender.sender.queue(server.getName())
                                                    .insert(DataMessage.newInstance(DataType.GAUTH, Channel.ACCOUNT)
                                                            .addProperty("player", player.getUniqueId()).getInstance().build());

                                            user.checkServer(0);
                                        }

                                        BungeeSender.sender.queue(server.getName())
                                                .insert(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT)
                                                        .addProperty("player", player.getUniqueId())
                                                        .addProperty("pin", false).getInstance().build());
                                    } else {
                                        if (pin.equalsIgnoreCase("error") || !manager.hasPin()) {
                                            BungeeSender.sender.queue(server.getName())
                                                    .insert(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT)
                                                            .addProperty("player", player.getUniqueId())
                                                            .addProperty("pin", false).getInstance().build());

                                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                                    UserAuthenticateEvent.Result.ERROR,
                                                    user.getModule(),
                                                    (manager.has2FA() ? messages.gAuthInstructions() : messages.logged()), null);
                                            ModulePlugin.callEvent(event);

                                            user.send(messages.prefix() + event.getAuthMessage());
                                            session.setPinLogged(true);
                                            if (manager.has2FA()) {
                                                session.set2FALogged(false);
                                            } else {
                                                session.set2FALogged(true);

                                                BungeeSender.sender.queue(server.getName())
                                                        .insert(DataMessage.newInstance(DataType.GAUTH, Channel.ACCOUNT)
                                                                .addProperty("player", player.getUniqueId()).getInstance().build());

                                                user.checkServer(0);
                                            }
                                        } else {
                                            if (!pin.equalsIgnoreCase("error") && manager.hasPin()) {
                                                UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                                        UserAuthenticateEvent.Result.ERROR,
                                                        user.getModule(),
                                                        "", null);
                                                ModulePlugin.callEvent(event);

                                                if (!event.getAuthMessage().isEmpty()) {
                                                    user.send(messages.prefix() + event.getAuthMessage());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });
                    c_Manager.addListener(Channel.ACCOUNT, DataType.JOIN, (server, data) -> {
                        if (data.has("player")) {
                            UUID uuid = UUID.fromString(data.get("player").getAsString());
                            ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);

                            if (player != null) {
                                User user = new User(player);
                                UserPostValidationEvent event = new UserPostValidationEvent(user.getModule(), name, null);
                                ModulePlugin.callEvent(event);
                            }
                        }
                    });
                    c_Manager.addListener(Channel.PLUGIN, DataType.PLAYER, (server, data) -> {
                        if (data.has("player_info")) {
                            ModulePlayer modulePlayer = StringUtils.loadUnsafe(data.get("player_info").getAsString());
                            if (modulePlayer != null) {
                                AccountManager manager = modulePlayer.getAccount();

                                if (manager != null) {
                                    AccountManager newManager = new PlayerAccount(manager.getUUID());
                                    MigrationManager migrationManager = new MigrationManager(manager, newManager);
                                    migrationManager.startMigration();
                                }
                            }
                        }
                    });
                    break;
            }

            initialized = true;
        });*/

        BungeeSender.sender = new BungeeDataSender();
        BungeeSender.useSocket = false;
        initialized = true;
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

                        if (instance instanceof Command) {
                            Command executor = (Command) instance;
                            plugin.getProxy().getPluginManager().registerCommand(plugin, executor);
                            registered.add("/" + command.toLowerCase());
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
        Metrics metrics = new Metrics(plugin, 6512);

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
    static void performVersionCheck() {
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
    }

    /**
     * Schedule the version check process
     */
    static void scheduleVersionCheck() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        SimpleScheduler timer = new SourceScheduler(plugin, config.getUpdaterOptions().getInterval(), SchedulerUnit.SECOND, true).multiThreading(true).endAction(Manager::performVersionCheck);
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
        SimpleScheduler timer = new SourceScheduler(plugin, 30, SchedulerUnit.SECOND, true).multiThreading(true).endAction(() -> {
            RemoteNotification system = new RemoteNotification();
            system.checkAlerts();

            Notification notification = system.getNotification();
            String text = notification.getNotification();
            int level = notification.getLevel();

            if (!last_notification_text.equals(text) && last_notification_level != level) {
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
                        ex.printStackTrace();
                    }
                    console.send("The current alert system requires some configuration options to be in a specified value. Custom config will be ignored for some variables", Level.WARNING);
                }
                if (notification.forceProxy()) {
                    try {
                        JarManager.changeField(CurrentPlatform.class, "fake_proxy", system.getRemoteProxyConfig());
                    } catch (Throwable ex) {
                        ex.printStackTrace();
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
                            eu.locklogin.api.common.communication.DataSender s = BungeeSender.sender;
                            if (BungeeSender.useSocket) {
                                s = new BungeeDataSender();
                            }

                            /*if (ServerDataStorage.needsRegister(info.getName())) {
                                s.queue(info.getName())
                                        .insert(DataMessage.newInstance(DataType.KEY, Channel.ACCESS)
                                                .addProperty("key", proxy.proxyKey())
                                                .addProperty("server", info.getName())
                                                .addProperty("socket", BungeeSender.useSocket).getInstance().build(), true);

                                            DataSender.send(info, DataSender.getBuilder(DataType.KEY, ACCESS_CHANNEL, player)
                                                    .addProperty("key", proxy.proxyKey())
                                                    .addProperty("server", info.getName())
                                                    .build());
                            }*/

                            //Scheduled for removal
                            if (ServerDataStorage.needsProxyKnowledge(info.getName())) {
                                s.queue(info.getName())
                                        .insert(DataMessage.newInstance(DataType.REGISTER, Channel.ACCESS)
                                                .addProperty("key", proxy.proxyKey())
                                                .addProperty("server", info.getName())
                                                .addProperty("socket", BungeeSender.useSocket).getInstance().build(), true);

                                            /*
                                            DataSender.send(info, DataSender.getBuilder(DataType.REGISTER, ACCESS_CHANNEL, player)
                                                    .addProperty("key", proxy.proxyKey())
                                                    .addProperty("server", info.getName())
                                                    //.addTextData(TokenGen.expiration("local_token").toString())
                                                    .build());*/
                            }
                        }

                        /*DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL, player)
                                .addProperty("key", proxy.proxyKey())
                                .addProperty("server", info.getName())
                                .addProperty("messages_yml", CurrentPlatform.getMessages().toString()).build());
                        DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL, player)
                                .addProperty("key", proxy.proxyKey())
                                .addProperty("server", info.getName())
                                .addProperty("config_yml", Config.manager.getConfiguration()).build());*/
                    }

                    CurrentPlatform.requestDataContainerUpdate();

                    /*DataSender.MessageData validation = getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER, player).build();
                    DataSender.send(player, validation);*/
                    BungeeSender.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.VALIDATION, Channel.ACCOUNT)
                            .addProperty("player", player.getUniqueId())
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
                    if (!session.isCaptchaLogged()) {
                        tmp_timer = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true);
                        tmp_timer.changeAction((second) -> player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(StringUtils.toColor(messages.captcha(session.getCaptcha()))))).start();
                    }

                    /*MessageData join = DataSender.getBuilder(DataType.JOIN, CHANNEL_PLAYER, player)
                            .addProperty("pass_login", session.isLogged())
                            .addProperty("2fa_login", session.is2FALogged())
                            .addProperty("pin_login", session.isPinLogged())
                            .addProperty("registered", manager.isRegistered()).build();
                    DataSender.send(player, join);*/
                    BungeeSender.sender.queue(BungeeSender.serverFromPlayer(player)).insert(
                            DataMessage.newInstance(DataType.JOIN, Channel.ACCOUNT)
                                    .addProperty("player", player.getUniqueId())
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

                    user.checkServer(0);

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

            //DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());
            BungeeSender.sender.queue(BungeeSender.serverFromPlayer(player))
                    .insert(DataMessage.newInstance(DataType.QUIT, Channel.ACCOUNT).addProperty("player", player.getUniqueId())
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

    /**
     * Get the version updater
     *
     * @return the version updater
     */
    public static VersionUpdater getUpdater() {
        return updater;
    }

    /**
     * Get if LockLogin has been initialized
     *
     * @return if the plugin has been initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
