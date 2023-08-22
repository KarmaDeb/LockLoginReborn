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

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.common.premium.DefaultPremiumDatabase;
import eu.locklogin.api.common.security.client.ProxyCheck;
import eu.locklogin.api.common.session.Session;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.session.online.SessionDataContainer;
import eu.locklogin.api.common.session.persistence.SessionKeeper;
import eu.locklogin.api.common.utils.filter.ConsoleFilter;
import eu.locklogin.api.common.utils.filter.PluginFilter;
import eu.locklogin.api.common.utils.other.ASCIIArtGenerator;
import eu.locklogin.api.common.utils.other.LockedAccount;
import eu.locklogin.api.common.utils.other.PlayerAccount;
import eu.locklogin.api.common.web.STFetcher;
import eu.locklogin.api.common.web.VersionDownloader;
import eu.locklogin.api.common.web.alert.Notification;
import eu.locklogin.api.common.web.alert.RemoteNotification;
import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.user.UserHookEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserUnHookEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.security.backup.BackupScheduler;
import eu.locklogin.api.util.enums.ManagerType;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.Main;
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.listener.*;
import eu.locklogin.plugin.bukkit.plugin.bungee.BungeeReceiver;
import eu.locklogin.plugin.bukkit.plugin.bungee.data.MessagePool;
import eu.locklogin.plugin.bukkit.premium.ProtocolListener;
import eu.locklogin.plugin.bukkit.util.LockLoginPlaceholder;
import eu.locklogin.plugin.bukkit.util.files.Config;
import eu.locklogin.plugin.bukkit.util.files.Message;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.files.data.RestartCache;
import eu.locklogin.plugin.bukkit.util.inventory.object.Button;
import eu.locklogin.plugin.bukkit.util.player.ClientVisor;
import eu.locklogin.plugin.bukkit.util.player.User;
import me.clip.placeholderapi.PlaceholderAPI;
import ml.karmaconfigs.api.bukkit.reflection.BarMessage;
import ml.karmaconfigs.api.common.karma.file.yaml.FileCopy;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.checker.VersionUpdater;
import ml.karmaconfigs.api.common.version.updater.VersionCheckType;
import net.milkbowl.vault.permission.Permission;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

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

        PlayerAccount.migrateV3();
        MessagePool.startCheckTask();

        setupFiles();
        //TokenGen.generate(plugin.getServer().getName());
        registerCommands();
        registerListeners();

        console.send(" ");
        console.send("&e-----------------------");

        try {
            JarManager.changeField(CurrentPlatform.class, "default_manager", PlayerAccount.class);
        } catch (Throwable ex) {
            ex.printStackTrace();
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

        SourceScheduler backup_scheduler = new SourceScheduler(plugin, Math.max(1, config.backup().getBackupPeriod()), SchedulerUnit.MINUTE, true);
        backup_scheduler.restartAction(() -> {
            BackupScheduler current_scheduler = CurrentPlatform.getBackupScheduler();
            current_scheduler.performBackup().whenComplete((id, error) -> {
                if (error != null) {
                    plugin.logger().scheduleLog(Level.GRAVE, error);
                    plugin.logger().scheduleLog(Level.INFO, "Failed to save backup {0}", id);
                    plugin.console().send("Failed to save backup {0}. See logs for more information", Level.GRAVE, id);
                } else {
                    plugin.console().send("Successfully created backup with id {0}", Level.INFO, id);
                }
            });

            int purge_days = config.backup().getPurgeDays();
            Instant today = Instant.now();
            Instant purge_target = today.minus(purge_days + 1, ChronoUnit.DAYS);

            current_scheduler.purge(purge_target).whenComplete((removed) -> {
                if (removed > 0) {
                    plugin.console().send("Purged {0} backups created {1} days ago", Level.INFO, removed, purge_days);
                }
            });
        });

        if (config.useVirtualID()) {
            CryptoFactory.loadVirtualID();
        } else {
            console.send("Virtual ID ( disabled by default) is disabled. You should enable it to enforce you clients security against database leaks", Level.GRAVE);
        }

        AccountManager t_manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, null);
        if (t_manager != null) {
            Set<AccountManager> accounts = t_manager.getAccounts();
            Set<AccountManager> nonLocked = new HashSet<>();
            for (AccountManager account : accounts) {
                LockedAccount locked = new LockedAccount(account.getUUID());
                if (!locked.isLocked() && account.isRegistered())
                    nonLocked.add(account);
            }

            SessionDataContainer.setRegistered(nonLocked.size());
        }

        trySync(TaskTarget.PLUGIN_HOOK, () -> {
            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                LockLoginPlaceholder placeholder = new LockLoginPlaceholder();
                if (placeholder.register()) {
                    console.send("Hooked and loaded placeholder expansion", Level.OK);
                } else {
                    console.send("Couldn't hook placeholder expansion", Level.GRAVE);
                }
            }
        });

        performVersionCheck();
        if (config.getUpdaterOptions().isEnabled()) {
            scheduleVersionCheck();
        }
        scheduleAlertSystem();

        Button.preCache();

        initPlayers();

        CurrentPlatform.setPrefix(config.getModulePrefix());

        /*plugin.async().queue("connect_web_services", () -> {
            License license = CurrentPlatform.getLicense();
            if (license != null) {
                String version = license.version();
                LicenseOwner owner = license.owner();
                LicenseExpiration expiration = license.expiration();

                InstantParser grant_parser = new InstantParser(expiration.granted());
                InstantParser expire_parser = new InstantParser(expiration.expiration());

                plugin.console().send("Successfully loaded your license: {0}", Level.INFO, version);
                plugin.console().send("------------------------------------------------------");
                plugin.console().send("&7License type: &e{0}", (license.isFree() ? "free" : "premium"));
                plugin.console().send("&7Licensed under:&e {0} ({1})", owner.name(), owner.contact());
                plugin.console().send("&7Licensed for: &e{0}&7 servers", license.max_proxies());
                plugin.console().send("&7License storage: &e{0}&7 bytes", license.backup_storage());
                plugin.console().send("&7Granted on: &e{0}", grant_parser.parse());
                plugin.console().send("&7Expires on: &e{0}", expire_parser.parse());
                if (expiration.hasExpiration()) {
                    if (expiration.isExpired()) {
                        plugin.console().send("&cYour license is expired; It will be marked as free until you renew it");
                    } else {
                        if (expiration.expireMonths() <= 0 && expiration.expireYears() <= 0) {
                            plugin.console().send("Your license will expire in {0} days, you should renew it before it expires", Level.WARNING, expiration.expireDays());
                        }

                        plugin.console().send("&7Expiration in: &e{0}&7 years&e {1}&7 months&e {2}&7 days &8(&e{3}&7 weeks&8)&e {4}&7 hours&e {5}&7 minutes and&e {6}&7 seconds",
                                expiration.expireYears(),
                                expiration.expireMonths(),
                                expiration.expireDays(),
                                expiration.expireWeeks(),
                                expiration.expireHours(),
                                expiration.expireMinutes(),
                                expiration.expireSeconds());
                    }
                } else {
                    plugin.console().send("&aYour license is a lifetime license");
                }
                plugin.console().send("------------------------------------------------------");
            } else {
                long time = System.currentTimeMillis();

                plugin.console().send("Failed to validate your license. It seems that LockLogin servers doesn't know about it", Level.GRAVE);
                Path license_file = plugin.getDataPath().resolve("cache").resolve("license.dat");
                if (Files.exists(license_file)) {
                    Path mod = plugin.getDataPath().resolve("cache").resolve("invalid_" + time + ".dat");

                    try {
                        Files.move(license_file, mod, StandardCopyOption.REPLACE_EXISTING);
                        plugin.console().send("Invalid license has been disabled, run /locklogin setup to setup a new valid free license", Level.INFO);
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            }

            console.send("Connecting to LockLogin web services (statistics and bungee communication), please wait...", Level.INFO);
            SocketClient socket = new LockLoginSocket();
            ConnectionManager c_Manager = new ConnectionManager(socket);

            c_Manager.connect(5).whenComplete((tries_amount) -> {
                if (tries_amount > 0) {
                    console.send("Connected to LockLogin web services after {0} tries", Level.WARNING, tries_amount);
                }

                switch (tries_amount) {
                    case -1:
                        //TODO: Allow configure tries amount from config and read that value
                        console.send("Failed to connect to LockLogin web services after 5 tries, giving up...", Level.WARNING);
                    case -2:
                        registerMetrics(null);
                        break;
                    case 0:
                        console.send("Connected to LockLogin web services successfully", Level.INFO);
                    default:
                        registerMetrics(socket);
                        console.send("Registering LockLogin web service listeners", Level.INFO);
                        c_Manager.addListener(Channel.ACCOUNT, DataType.PIN, (data) -> {
                            if (data.has("pin_input") && data.has("player")) {
                                UUID uuid = UUID.fromString(data.get("player").getAsString());
                                Player player = plugin.getServer().getPlayer(uuid);

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
                                            session.set2FALogged(!manager.has2FA());
                                        } else {
                                            if (pin.equalsIgnoreCase("error") || !manager.hasPin()) {
                                                UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                                        UserAuthenticateEvent.Result.ERROR,
                                                        user.getModule(),
                                                        (manager.has2FA() ? messages.gAuthInstructions() : messages.logged()), null);
                                                ModulePlugin.callEvent(event);

                                                user.send(messages.prefix() + event.getAuthMessage());
                                                session.setPinLogged(true);
                                                session.set2FALogged(!manager.has2FA());
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
                        c_Manager.addListener(Channel.ACCOUNT, DataType.JOIN, (data) -> {
                            if (data.has("player")) {
                                UUID uuid = UUID.fromString(data.get("player").getAsString());
                                Player player = plugin.getServer().getPlayer(uuid);

                                if (player != null) {
                                    User user = new User(player);
                                    UserPostValidationEvent event = new UserPostValidationEvent(user.getModule(), name, null);
                                    ModulePlugin.callEvent(event);
                                }
                            }
                        });
                        c_Manager.addListener(Channel.PLUGIN, DataType.PLAYER, (data) -> {
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
            });

            if (config.isBungeeCord())
                registerBungee(socket, c_Manager);

            if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
                RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
                if (rsp != null) {
                    Permission permission = rsp.getProvider();
                    if (permission.isEnabled()) {
                        plugin.console().send("Detected permission provider {0}. LockLogin will remove all player permissions and OP when the client connects and restore them after a success login for better security", Level.INFO, rsp.getPlugin().getName());
                    }
                }
            }
            if (plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib") && !CurrentPlatform.isOnline()) {
                plugin.console().send("ProtocolLib detected, trying to toggle premium mode support", Level.INFO);
                try {
                    ProtocolListener.register();
                    CurrentPlatform.setPremiumDatabase(new DefaultPremiumDatabase());
                } catch (Throwable ignored) {}
            }
        });*/

        registerMetrics();
        if (config.isBungeeCord())
            registerBungee();

        if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
            if (rsp != null) {
                Permission permission = rsp.getProvider();
                if (permission.isEnabled()) {
                    plugin.console().send("Detected permission provider {0}. LockLogin will remove all player permissions and OP when the client connects and restore them after a success login for better security", Level.INFO, rsp.getPlugin().getName());
                }
            }
        }
        if (plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib") && !CurrentPlatform.isOnline()) {
            plugin.console().send("ProtocolLib detected, trying to toggle premium mode support", Level.INFO);
            try {
                ProtocolListener.register();
                CurrentPlatform.setPremiumDatabase(new DefaultPremiumDatabase());
            } catch (Throwable ignored) {}
        }

        initialized = true;
    }

    public static void terminate() {
        initialized = false;
        endPlayers();

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

        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            LockLoginPlaceholder placeholder = new LockLoginPlaceholder();
            if (placeholder.isRegistered()) {
                if (placeholder.unregister()) {
                    console.send("Unhooked placeholder expansion", Level.OK);
                } else {
                    console.send("Couldn't un-hook placeholder expansion", Level.GRAVE);
                }
            }
        }

        System.out.println();
        artGen.print("\u001B[31m", "LockLogin", size, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, character);
        console.send("&eversion:&6 {0}", version);
        console.send(" ");
        console.send("&e-----------------------");
    }

    static void registerBungee() {
        Messenger messenger = plugin.getServer().getMessenger();
        BungeeReceiver receiver = new BungeeReceiver();

        PluginMessageListenerRegistration registration_account = messenger.registerIncomingPluginChannel(plugin, "ll:account", receiver);
        messenger.registerOutgoingPluginChannel(plugin, "ll:account");
        PluginMessageListenerRegistration registration_plugin = messenger.registerIncomingPluginChannel(plugin, "ll:plugin", receiver);
        PluginMessageListenerRegistration access_plugin = messenger.registerIncomingPluginChannel(plugin, "ll:access", receiver);
        messenger.registerOutgoingPluginChannel(plugin, "ll:access");

        if (registration_account.isValid() && registration_plugin.isValid() && access_plugin.isValid()) {
            console.send("Registered plugin message listeners", Level.OK);
        } else {
            console.send("Something went wrong while trying to register message listeners, things may not work properly", Level.GRAVE);
        }
    }

    /**
     * Register plugin commands
     */
    static void registerCommands() {
        Set<String> unregistered = new LinkedHashSet<>();
        Set<String> registered = new HashSet<>();

        PluginConfiguration config = CurrentPlatform.getConfiguration();

        for (Class<?> clazz : SystemCommand.manager.recognizedClasses()) {
            if (clazz.isAnnotationPresent(SystemCommand.class)) {
                try {
                    String command = SystemCommand.manager.getDeclaredCommand(clazz);
                    List<String> aliases = SystemCommand.manager.getDeclaredAliases(clazz);
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
                            registered.add("/" + alias.toLowerCase());
                        continue;
                    }

                    Object instance = clazz.getDeclaredConstructor().newInstance();

                    if (instance instanceof CommandExecutor) {
                        CommandExecutor executor = (CommandExecutor) instance;
                        pluginCMD.setExecutor(executor);
                        pluginCMD.setAliases(aliases);

                        registered.add("/" + command.toLowerCase());
                        for (String alias : pluginCMD.getAliases())
                            registered.add("/" + alias.toLowerCase());
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (!unregistered.isEmpty()) {
            console.send(properties.getProperty("command_register_problem", "Failed to register command(s): {0}"), Level.GRAVE, setToString(unregistered));
            console.send(properties.getProperty("plugin_error_disabling", "Disabling plugin due an internal error"), Level.INFO);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        } else {
            console.send(properties.getProperty("plugin_filter_initialize", "Initializing console filter to protect user data"), Level.INFO);

            try {
                Logger coreLogger = (Logger) LogManager.getRootLogger();
                ConsoleFilter filter = new ConsoleFilter(registered);

                coreLogger.addFilter(filter);
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
                logger.scheduleLog(Level.INFO, "Failed to register console filter");

                console.send(properties.getProperty("plugin_filter_error", "An error occurred while initializing console filter, check logs for more info"), Level.GRAVE);
                console.send(properties.getProperty("plugin_error_disabling", "Disabling plugin due an internal error"), Level.INFO);
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        }
    }

    /**
     * Setup the plugin files
     */
    static void setupFiles() {
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

    static void registerMetrics() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        Metrics metrics = new Metrics(plugin, 6513);

        if (config.shareBStats()) {
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

        /*if (BukkitServer.isOver(Version.v1_8)) {
            Listener interact_new = new InteractListenerNew();
            plugin.getServer().getPluginManager().registerEvents(interact_new, plugin);
        }*/
    }

    /**
     * Load the plugin cache if exists
     */
    static void loadCache() {
        RestartCache cache = new RestartCache();
        cache.loadBungeeKey();
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
                            for (Player player : plugin.getServer().getOnlinePlayers()) {
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

                                for (Player player : plugin.getServer().getOnlinePlayers()) {
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

        SimpleScheduler timer = new SourceScheduler(plugin, config.getUpdaterOptions().getInterval(), SchedulerUnit.SECOND, true).multiThreading(true).endAction(Manager::performVersionCheck);
        if (config.getUpdaterOptions().isEnabled())
            timer.start();

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
                /*
                Spigot. Just ignore proxy configuration

                if (notification.forceProxy()) {

                }*/
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
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            PluginMessages messages = CurrentPlatform.getMessages();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                User user = new User(player);
                ClientSession session = user.getSession();
                InetSocketAddress ip = player.getAddress();

                if (!config.isBungeeCord()) {
                    ProxyCheck proxy = new ProxyCheck(ip);
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

                    BarMessage bar = null;
                    if (config.captchaOptions().isEnabled()) {
                        String barMessage = messages.captcha(session.getCaptcha());
                        try {
                            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null)
                                barMessage = PlaceholderAPI.setPlaceholders(player, barMessage);
                        } catch (Throwable ignored) {
                        }

                        bar = new BarMessage(player, barMessage);
                        if (!session.isCaptchaLogged())
                            bar.send(true);
                    }

                    BarMessage finalBar = bar;
                    SessionCheck<Player> check = user.getChecker().whenComplete(() -> {
                        user.restorePotionEffects();

                        if (finalBar != null) {
                            finalBar.setMessage("");
                            finalBar.stop();
                        }
                    });
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, check);
                }

                if (player.getLocation().getBlock().getType().name().contains("PORTAL"))
                    user.setTempSpectator(true);

                if (config.hideNonLogged()) {
                    ClientVisor visor = new ClientVisor(player);
                    visor.toggleView();
                }

                Event event = new UserHookEvent(user.getModule(), null);
                ModulePlugin.callEvent(event);
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
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            User user = new User(player);

            if (user.isLockLoginUser()) {
                Config config = new Config();
                if (!config.isBungeeCord()) {
                    SessionKeeper keeper = new SessionKeeper(user.getModule());
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
            visor.forceView();

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
