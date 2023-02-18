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
import eu.locklogin.api.common.security.backup.BackupTask;
import eu.locklogin.api.common.security.client.ProxyCheck;
import eu.locklogin.api.common.session.Session;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.session.online.SessionDataContainer;
import eu.locklogin.api.common.session.persistence.SessionKeeper;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.InstantParser;
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
import eu.locklogin.api.common.web.services.metric.PluginMetricsService;
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
import eu.locklogin.api.plugin.license.License;
import eu.locklogin.api.plugin.license.LicenseExpiration;
import eu.locklogin.api.plugin.license.LicenseOwner;
import eu.locklogin.api.security.backup.BackupScheduler;
import eu.locklogin.api.util.enums.ManagerType;
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
import io.socket.client.Ack;
import io.socket.client.Socket;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.element.multi.KarmaMap;
import ml.karmaconfigs.api.common.karma.file.yaml.FileCopy;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.checker.VersionUpdater;
import ml.karmaconfigs.api.common.version.updater.VersionCheckType;
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
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

import static eu.locklogin.plugin.bungee.LockLogin.*;

public final class Manager {

    private static String last_notification_text = "";
    private static int last_notification_level = 0;

    private static VersionUpdater updater = null;
    private static int changelog_requests = 0;
    private static int updater_id = 0;
    private static int alert_id = 0;

    private static boolean initialized = false;

    public static BiFunction<DataMessage, ServerInfo, Void> sendFunction;
    public static BiFunction<DataMessage, ServerInfo, Void> sendTopFunction;
    public static BiFunction<DataMessage, ServerInfo, Void> sendSecondaryFunction;
    public static BiFunction<DataMessage, ServerInfo, Void> sendSecondaryTopFunction;
    public static Function<String, Void> unlockFunction;

    public static BiFunction<String, String, Void> mapFunction;
    public static Function<String, Void> unlockSecondaryFunction;

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

        /*SourceScheduler backup_scheduler = new SourceScheduler(plugin, Math.max(1, config.backup().getBackupPeriod()), SchedulerUnit.MINUTE, true);
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
        });*/

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
        SocketClient socket = new LockLoginSocket();
        ProxyDataSender pds = new ProxyDataSender(socket);
        ConnectionManager c_Manager = new ConnectionManager(socket, pds);

        BungeeSender sender = new BungeeSender();

        sendFunction = (dataMessage, serverInfo) -> {
            if (BungeeSender.isForceBungee(serverInfo)) {
                if (sender.secondarySender != null) {
                    sender.secondarySender.queue(serverInfo.getName()).insert(dataMessage.build());
                }
            } else {
                if (sender.sender != null) {
                    sender.sender.queue(serverInfo.getName()).insert(dataMessage.build());
                }
            }
            return null;
        };
        sendTopFunction = (dataMessage, serverInfo) -> {
            if (BungeeSender.isForceBungee(serverInfo)) {
                if (sender.secondarySender != null) {
                    sender.secondarySender.queue(serverInfo.getName()).insert(dataMessage.build(), true);
                }
            } else {
                if (sender.sender != null) {
                    sender.sender.queue(serverInfo.getName()).insert(dataMessage.build(), true);
                }
            }
            return null;
        };

        sendSecondaryFunction = (dataMessage, serverInfo) -> {
            if (sender.secondarySender != null) {
                sender.secondarySender.queue(serverInfo.getName()).insert(dataMessage.build());
            }
            return null;
        };
        sendSecondaryTopFunction = (dataMessage, serverInfo) -> {
            if (sender.secondarySender != null) {
                sender.secondarySender.queue(serverInfo.getName()).insert(dataMessage.build(), true);
            }
            return null;
        };

        unlockFunction = (server) -> {
            ServerInfo info = plugin.getProxy().getServerInfo(server);

            if (BungeeSender.isForceBungee(info)) {
                if (sender.secondarySender != null) {
                    sender.secondarySender.queue(server).unlock();
                }
            } else {
                if (sender.sender != null) {
                    sender.sender.queue(server).unlock();
                }
            }
            return null;
        };
        unlockSecondaryFunction = (server) -> {
            if (sender.secondarySender != null) {
                sender.secondarySender.queue(server).unlock();
            }
            return null;
        };

        mapFunction = (server, id) -> {
            if (sender.sender instanceof ProxyDataSender) {
                ((ProxyDataSender) sender.sender).server_maps.put(server, id);
            }
            if (sender.secondarySender instanceof ProxyDataSender) {
                ((ProxyDataSender) sender.secondarySender).server_maps.put(server, id);
            }
            return null;
        };

        plugin.async().queue("connect_web_services", () -> {
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

            BungeeDataSender bs = new BungeeDataSender();
            final Map<String, String> internalMap = new ConcurrentHashMap<>();

            c_Manager.connect(5, (name, id, hash) -> {
                ServerInfo server = plugin.getProxy().getServerInfo(name);
                if (server != null) {
                    KarmaMain hash_store = new KarmaMain(plugin, ".hashes", "cache");
                    if (!hash_store.exists())
                        hash_store.create();

                    ServerDataStorage.setProxyRegistered(name);

                    pds.server_maps.put(name, id);
                    pds.queue(name).unlock();

                    InetSocketAddress isa = (InetSocketAddress) server.getSocketAddress();

                    KarmaMap data = new KarmaMap();
                    data.put("name", name);
                    data.put("address", isa.getHostString());
                    data.put("port", isa.getPort());
                    //We store this information to detect changes when loading
                    hash_store.set(hash, data);
                    hash_store.save();

                    internalMap.put(name, hash);

                    console.send("Server {0} has been connected to the proxy", Level.INFO, name);
                }
            }, sender, bs).whenComplete((tries_amount) -> {
                if (tries_amount > 0) {
                    console.send("Connected to LockLogin web services after {0} tries", Level.WARNING, tries_amount);
                }

                plugin.console().send("Marking server as initialized", Level.OK);
                initialized = true;

                switch (tries_amount) {
                    case -1:
                        //TODO: Allow configure tries amount from config and read that value
                        console.send("Failed to connect to LockLogin web services after 5 tries, giving up...", Level.WARNING);
                    case -2:
                        sender.sender = bs;
                        sender.secondarySender = sender.sender; //For stability reasons, should never be null
                        //BungeeSender.useSocket = false;

                        initPlayers(sender);
                        registerMetrics(null);
                        break;
                    case 0:
                        console.send("Connected to LockLogin web services successfully", Level.INFO);
                    default:
                        registerMetrics(socket);
                        KarmaMain hashes = new KarmaMain(plugin, ".hashes", "cache");
                        if (hashes.exists()) {
                            for (String hash : hashes.getKeys()) {
                                hash = hash.replace("main.", "");
                                KarmaMap data = (KarmaMap) hashes.get(hash);

                                String name = data.get("name").asString();
                                String address = data.get("address").asString();
                                int port = data.get("port").asInteger();

                                ServerInfo info = plugin.getProxy().getServerInfo(name);
                                if (info != null) {
                                    InetSocketAddress isa = (InetSocketAddress) info.getSocketAddress();

                                    if (isa.getHostString().equals(address) && isa.getPort() == port) {
                                        Socket connection = socket.client();
                                        JsonObject request = new JsonObject();

                                        request.addProperty("hash", hash);
                                        request.addProperty("name", name);
                                        connection.emit("init_auth", request, (Ack) (response) -> {
                                            try {
                                                Gson gson = new GsonBuilder().create();
                                                JsonObject r = gson.fromJson(String.valueOf(response[0]), JsonObject.class);

                                                if (r.get("success").getAsBoolean()) {
                                                    plugin.console().send("Trying to register {0}", Level.INFO, name);
                                                } else {
                                                    String reason = r.get("message").getAsString();
                                                    plugin.console().send("Failed to register {0} ({1})", Level.GRAVE, name, reason);
                                                }
                                            } catch (Throwable ex) {
                                                logger.scheduleLog(Level.GRAVE, ex);
                                            }
                                        });
                                    } else {
                                        hashes.unset(hash); //Data changed, forget it
                                    }
                                }
                            }
                        }

                        sender.sender = pds;
                        sender.secondarySender = bs;
                        BungeeSender.useSocket = true;

                        initPlayers(sender);

                        c_Manager.onServerConnected((name, id, hash) -> {
                            ServerInfo server = plugin.getProxy().getServerInfo(name);
                            if (server != null) {
                                KarmaMain hash_store = new KarmaMain(plugin, ".hashes", "cache");
                                if (!hash_store.exists())
                                    hash_store.create();

                                ServerDataStorage.setProxyRegistered(name);

                                pds.server_maps.put(name, id);
                                pds.queue(name).unlock();

                                InetSocketAddress isa = (InetSocketAddress) server.getSocketAddress();

                                KarmaMap data = new KarmaMap();
                                data.put("name", name);
                                data.put("address", isa.getHostString());
                                data.put("port", isa.getPort());
                                //We store this information to detect changes when loading
                                hash_store.set(hash, data);
                                hash_store.save();

                                internalMap.put(name, hash);

                                console.send("Server {0} has been connected to the proxy", Level.INFO, name);
                            }
                        });
                        c_Manager.onServerDisconnected((name, reason) -> {
                            ServerInfo server = plugin.getProxy().getServerInfo(name);
                            if (server != null) {
                                ServerDataStorage.removeProxyRegistered(name);
                                console.send("Server {0} has been disconnected ({1})", Level.INFO, name, reason);

                                Socket connection = socket.client();

                                JsonObject request = new JsonObject();
                                request.addProperty("hash", internalMap.remove(name));
                                request.addProperty("name", name);

                                connection.emit("init_auth", request);
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
                                                sender.sender.queue(server.getName())
                                                        .insert(DataMessage.newInstance(DataType.GAUTH, Channel.ACCOUNT, player)
                                                                .getInstance().build());

                                                user.checkServer(0);
                                            }

                                            sender.sender.queue(server.getName())
                                                    .insert(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT, player)
                                                            .addProperty("pin", false).getInstance().build());
                                        } else {
                                            if (pin.equalsIgnoreCase("error") || !manager.hasPin()) {
                                                sender.sender.queue(server.getName())
                                                        .insert(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT, player)
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

                                                    sender.sender.queue(server.getName())
                                                            .insert(DataMessage.newInstance(DataType.GAUTH, Channel.ACCOUNT, player)
                                                                    .getInstance().build());

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

                                System.out.println("Validated player: " + player.getUniqueId());

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
            });

            plugin.async().queue("setup_placeholder_data", () -> {
                AccountManager acc_manager = CurrentPlatform.getAccountManager(ManagerType.CUSTOM, null);
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
                                        sender.sender.queue(server.getName())
                                                .insert(DataMessage.newInstance(DataType.LOGGED, Channel.PLUGIN, server.getPlayers().stream().findAny().orElse(null))
                                                        .addProperty("login_count", SessionDataContainer.getLogged()).getInstance().build());
                                    }
                                    break;
                                case REGISTER:
                                    for (ServerInfo server : servers) {
                                        sender.sender.queue(server.getName())
                                                .insert(DataMessage.newInstance(DataType.REGISTERED, Channel.PLUGIN, server.getPlayers().stream().findAny().orElse(null))
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
            });
        });

        end = unused -> {
            endPlayers(sender);
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
    static void registerMetrics(final SocketClient s) {
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

        if (config.sharePlugin()) {
            PluginMetricsService service = new PluginMetricsService(plugin, s);
            service.start();
        } else {
            console.send("Plugin metrics are disabled. Data will still be sent but won't be public", Level.INFO);
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
    static void initPlayers(final BungeeSender sender) {
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
                            if (BungeeSender.useSocket) {
                                Manager.sendSecondaryTopFunction
                                        .apply(DataMessage.newInstance(DataType.REGISTER, Channel.ACCESS, player)
                                                        .addProperty("key", proxy.proxyKey())
                                                        .addProperty("server", info.getName())
                                                        .addProperty("socket", BungeeSender.useSocket).getInstance(),
                                                info);
                            } else {
                                Manager.sendTopFunction
                                        .apply(DataMessage.newInstance(DataType.REGISTER, Channel.ACCESS, player)
                                                        .addProperty("key", proxy.proxyKey())
                                                        .addProperty("server", info.getName())
                                                        .addProperty("socket", BungeeSender.useSocket).getInstance(),
                                                info);
                            }
                        }
                    }

                    CurrentPlatform.requestDataContainerUpdate();

                    sender.sender.queue(BungeeSender.serverFromPlayer(player).getName()).insert(DataMessage.newInstance(DataType.VALIDATION, Channel.ACCOUNT, player)
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

                    sender.sender.queue(BungeeSender.serverFromPlayer(player).getName()).insert(
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
    static void endPlayers(final BungeeSender sender) {
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            User user = new User(player);

            SessionKeeper keeper = new SessionKeeper(user.getModule());
            keeper.store();

            ClientSession session = user.getSession();
            session.invalidate();
            session.setLogged(false);
            session.setPinLogged(false);
            session.set2FALogged(false);

            sender.sender.queue(BungeeSender.serverFromPlayer(player).getName())
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
