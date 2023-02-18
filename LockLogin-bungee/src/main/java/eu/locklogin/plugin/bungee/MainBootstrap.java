package eu.locklogin.plugin.bungee;

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.common.security.AllowedCommand;
import eu.locklogin.api.common.session.online.SessionDataContainer;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.common.utils.dependencies.DependencyManager;
import eu.locklogin.api.common.utils.dependencies.PluginDependency;
import eu.locklogin.api.common.utils.plugin.MessageQueue;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.common.web.STFetcher;
import eu.locklogin.api.common.web.services.LockLoginSocket;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.LoadRule;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginStatusChangeEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.ActionBarSender;
import eu.locklogin.api.module.plugin.client.MessageSender;
import eu.locklogin.api.module.plugin.client.OpContainer;
import eu.locklogin.api.module.plugin.client.TitleSender;
import eu.locklogin.api.module.plugin.client.permission.PermissionContainer;
import eu.locklogin.api.module.plugin.client.permission.PermissionObject;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.server.TargetServer;
import eu.locklogin.api.plugin.PluginLicenseProvider;
import eu.locklogin.api.plugin.license.License;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.plugin.Manager;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.file.yaml.FileCopy;
import ml.karmaconfigs.api.common.karma.loader.BruteLoader;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static eu.locklogin.plugin.bungee.LockLogin.*;

public class MainBootstrap {

    private final Main loader;

    public MainBootstrap(final Plugin main) {
        loader = (Main) main;

        try {
            JarManager.changeField(CurrentPlatform.class, "current_appender", new BruteLoader((URLClassLoader) main.getClass().getClassLoader()));
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public void enable() {
        for (Dependency pluginDependency : Dependency.values()) {
            if (!pluginDependency.equals(Dependency.APACHE_COMMONS)) {
                PluginDependency dependency = pluginDependency.getAsDependency();

                if (FileInfo.showChecksums(null)) {
                    console.send("&7----------------------");
                    console.send("");
                    console.send("&bDependency: &3{0}", dependency.getName());
                    console.send("&bType&8/&eCurrent&8/&aFetched");
                    console.send("&bAdler32 &8- {0} &8- &a{1}", dependency.getAdlerCheck(), ChecksumTables.getAdler(dependency));
                    console.send("&bCRC32 &8- {0} &8- &a{1}", dependency.getCRCCheck(), ChecksumTables.getCRC(dependency));
                    console.send("");
                    console.send("&7----------------------");
                }

                JarManager manager = new JarManager(dependency);
                manager.process(false);
                if (dependency.isHighPriority()) {
                    manager.downloadAndInject();
                }
            }
        }
        JarManager.downloadAll();
        DependencyManager.loadDependencies();

        SimpleScheduler check_scheduler = new SourceScheduler(plugin, 10, SchedulerUnit.SECOND, true);
        check_scheduler.restartAction(() -> {
            for (ServerInfo server : plugin.getProxy().getServers().values()) {
                if (BungeeSender.isForceBungee(server) || !BungeeSender.useSocket) {
                    if (!ServerDataStorage.needsProxyKnowledge(server.getName())) {
                        server.ping((result, error) -> {
                            if (error != null) {
                                ServerDataStorage.removeProxyRegistered(server.getName());
                                plugin.console().send("Failed to ping server {0}. Marking it as offline", Level.WARNING, server.getName());
                            }
                        });
                    }
                }
            }
        });

        check_scheduler.start();

        console.send("&aUsing KarmaAPI version {0}, compiled at {1} for jdk {2}", KarmaAPI.getVersion(), KarmaAPI.getBuildDate(), KarmaAPI.getCompilerVersion());

        STFetcher fetcher = new STFetcher();
        fetcher.check();

        CurrentPlatform.setOnDataContainerUpdate(() -> {
            for (ServerInfo server : plugin.getProxy().getServers().values()) {
                Manager.sendFunction.apply(DataMessage.newInstance(DataType.LOGGED, Channel.PLUGIN, server.getPlayers().stream().findAny().orElse(null))
                        .addProperty("login_count", SessionDataContainer.getLogged()).getInstance(), server);

                Manager.sendFunction.apply(DataMessage.newInstance(DataType.REGISTERED, Channel.PLUGIN, server.getPlayers().stream().findAny().orElse(null))
                        .addProperty("register_count", SessionDataContainer.getRegistered()).getInstance(), server);
            }
        });
        Consumer<MessageSender> onMessage = messageSender -> {
            if (messageSender.getSender() instanceof ModulePlayer) {
                ModulePlayer mp = (ModulePlayer) messageSender.getSender();
                ProxiedPlayer player = mp.getPlayer();

                if (player != null) {
                    User user = new User(player);
                    user.send(messageSender.getMessage());
                }
            }
        };
        Consumer<ActionBarSender> onActionBar = messageSender -> {
            ProxiedPlayer player = messageSender.getPlayer().getPlayer();

            if (player != null) {
                User user = new User(player);

                if (!StringUtils.isNullOrEmpty(messageSender.getMessage())) {
                    TextComponent component = new TextComponent(messageSender.getMessage());
                    user.send(component);
                } else {
                    TextComponent component = new TextComponent("");
                    user.send(component);
                }
            }
        };
        Consumer<TitleSender> onTitle = messageSender -> {
            ProxiedPlayer player = messageSender.getPlayer().getPlayer();

            if (player != null) {
                User user = new User(player);

                if (StringUtils.isNullOrEmpty(messageSender.getTitle()) && StringUtils.isNullOrEmpty(messageSender.getSubtitle()))
                    user.send("", "", 0, 0, 0);

                user.send(messageSender.getTitle(), messageSender.getSubtitle(), messageSender.getFadeOut(), messageSender.getKeepIn(), messageSender.getHideIn());
            }
        };
        Consumer<MessageSender> onKick = messageSender -> {
            SimpleScheduler scheduler = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, false).multiThreading(false);
            scheduler.endAction(() -> plugin.sync().queue("kick_request", () -> {
                if (messageSender.getSender() instanceof ModulePlayer) {
                    ModulePlayer mp = (ModulePlayer) messageSender.getSender();
                    ProxiedPlayer player = mp.getPlayer();

                    if (player != null) {
                        User user = new User(player);
                        user.kick(messageSender.getMessage());
                    }
                }
            })).start();
        };
        Consumer<ModulePlayer> onLogin = modulePlayer -> {
            UUID id = modulePlayer.getUUID();

            ProxiedPlayer player = loader.getProxy().getPlayer(id);
            if (player != null) {
                User user = new User(player);
                ClientSession session = user.getSession();

                if (!session.isLogged() || !session.isTempLogged()) {
                    session.setCaptchaLogged(true);
                    session.setLogged(true);
                    session.setPinLogged(true);
                    session.set2FALogged(true);

                    Manager.sendFunction.apply(DataMessage.newInstance(DataType.SESSION, Channel.ACCOUNT, player)
                            .getInstance(), BungeeSender.serverFromPlayer(player));

                    Manager.sendFunction.apply(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT, player)
                            .addProperty("pin", false).getInstance(), BungeeSender.serverFromPlayer(player));

                    Manager.sendFunction.apply(DataMessage.newInstance(DataType.GAUTH, Channel.ACCOUNT, player)
                            .getInstance(), BungeeSender.serverFromPlayer(player));

                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.API,
                            UserAuthenticateEvent.Result.SUCCESS,
                            modulePlayer,
                            "",
                            null);
                    ModulePlugin.callEvent(event);

                    user.checkServer(0);
                    user.send(event.getAuthMessage());
                }
            }
        };
        Consumer<ModulePlayer> onClose = modulePlayer -> {
            UUID id = modulePlayer.getUUID();

            ProxiedPlayer player = loader.getProxy().getPlayer(id);
            if (player != null) {
                User user = new User(player);
                user.performCommand("account close");
            }
        };
        Consumer<PermissionContainer> hasPermission = container -> {
            UUID id = container.getAttachment().getUUID();

            ProxiedPlayer player = loader.getProxy().getPlayer(id);
            if (player != null) {
                PermissionObject permission = container.getPermission();

                switch (permission.getCriteria()) {
                    case TRUE:
                        container.setResult(!player.hasPermission("!" + permission.getPermission()));
                        break;
                    case FALSE:
                    case OP:
                    default:
                        container.setResult(player.hasPermission(permission.getPermission()));
                }
            }
        };
        Consumer<OpContainer> opContainer = container -> {
            UUID id = container.getAttachment().getUUID();

            ProxiedPlayer player = loader.getProxy().getPlayer(id);
            if (player != null) {
                container.setResult(player.hasPermission("*") || player.hasPermission("'*'"));
            }
        };

        BiConsumer<String, Set<ModulePlayer>> onPlayers = (name, players) -> {
            ServerInfo info = plugin.getProxy().getServerInfo(name);
            if (info != null) {
                info.getPlayers().forEach((player) -> {
                    User user = new User(player);
                    players.add(user.getModule());
                });
            }
        };

        try {
            JarManager.changeField(ModulePlayer.class, "onChat", onMessage);
            JarManager.changeField(ModulePlayer.class, "onBar", onActionBar);
            JarManager.changeField(ModulePlayer.class, "onTitle", onTitle);
            JarManager.changeField(ModulePlayer.class, "onKick", onKick);
            JarManager.changeField(ModulePlayer.class, "onLogin", onLogin);
            JarManager.changeField(ModulePlayer.class, "onClose", onClose);
            JarManager.changeField(ModulePlayer.class, "hasPermission", hasPermission);
            JarManager.changeField(ModulePlayer.class, "opContainer", opContainer);

            JarManager.changeField(TargetServer.class, "onPlayers", onPlayers);
        } catch (Throwable ignored) {}

        LockLogin.logger.scheduleLog(Level.OK, "LockLogin initialized and all its dependencies has been loaded");

        File[] moduleFiles = LockLogin.getLoader().getDataFolder().listFiles();
        if (moduleFiles != null) {
            List<File> files = Arrays.asList(moduleFiles);
            Iterator<File> iterator = files.iterator();
            do {
                File file = iterator.next();
                LockLogin.getLoader().loadModule(file, LoadRule.PREPLUGIN);
            } while (iterator.hasNext());
        }

        Event event = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.LOAD, null);
        ModulePlugin.callEvent(event);

        if (moduleFiles != null) {
            List<File> files = Arrays.asList(moduleFiles);
            Iterator<File> iterator = files.iterator();
            do {
                File file = iterator.next();
                LockLogin.getLoader().loadModule(file, LoadRule.POSTPLUGIN);
            } while (iterator.hasNext());
        }

        plugin.getProxy().getConfig().getListeners().forEach((info) -> {
            InetSocketAddress isa = (InetSocketAddress) info.getSocketAddress();
            CurrentPlatform.init(isa.getPort());
        });

        CurrentPlatform.setLicenseProvider(new LockLoginSocket());

        Path license = plugin.getDataPath().resolve("cache").resolve("license.dat");
        if (!Files.exists(license)) {
            plugin.console().send("License file not found, trying to export from internal", Level.WARNING);
            InputStream internal = getClass().getResourceAsStream("/license.dat");
            if (internal != null) {
                try {
                    KarmaMain mn = new KarmaMain(internal);
                    String data = mn.get("license").getAsString();

                    byte[] real = Base64.getDecoder().decode(data);
                    PathUtilities.create(license);

                    Files.write(license, real);
                } catch (Throwable ignored) {}
            } else {
                plugin.getProxy().getScheduler().schedule(plugin, () -> {
                    License installed = CurrentPlatform.getLicense();
                    if (installed == null) {
                        PluginConfiguration config = CurrentPlatform.getConfiguration();
                        if (config != null && config.isBungeeCord()) {
                            plugin.console().send("IMPORTANT! Please synchronize this server with your proxy license (/locklogin sync) or install a license (/locklogin install) and synchronize the installed license with your network to enable LockLogin BungeeCord", Level.GRAVE);
                        }
                    }
                }, 0, 30, TimeUnit.SECONDS);
            }
        }

        if (Files.exists(license)) {
            PluginLicenseProvider provider = CurrentPlatform.getLicenseProvider();
            plugin.console().send("Validating plugin license, please wait...", Level.INFO);
            License provided = provider.fetch(license);

            if (provided != null) {
                provided.setInstallLocation(plugin.getDataPath().resolve("cache"));
                try {
                    JarManager.changeField(CurrentPlatform.class, "current_license", provided);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        } else {

        }

        AllowedCommand.scan();
        Manager.initialize();

        SimpleScheduler scheduler = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true).multiThreading(true);
        scheduler.restartAction(() -> {
            for (TargetServer server : CurrentPlatform.getServer().getServers()) {
                MessageQueue queue = fetchQueue(server);
                byte[] data = queue.previewMessage();

                if (data != null) {
                    data = queue.nextMessage();

                    for (ModulePlayer player : server.getOnlinePlayers()) {
                        if (player.isPlaying()) {
                            if (data != null) {
                                Manager.sendFunction.apply(DataMessage.newInstance(DataType.LISTENER, Channel.PLUGIN, player.getPlayer())
                                        .addProperty("other", new String(Base64.getEncoder().encode(data))).getInstance(),
                                        BungeeSender.serverFromPlayer(player.getPlayer()));

                                queue.nextMessage();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    public void disable() {
        Event event = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UNLOAD, null);
        ModulePlugin.callEvent(event);

        File[] moduleFiles = LockLogin.getLoader().getDataFolder().listFiles();
        if (moduleFiles != null) {
            List<File> files = Arrays.asList(moduleFiles);
            Iterator<File> iterator = files.iterator();
            do {
                File file = iterator.next();
                if (file.isFile()) {
                    LockLogin.getLoader().unloadModule(file);
                }
            } while (iterator.hasNext());
        }

        Manager.terminate();
    }
}
