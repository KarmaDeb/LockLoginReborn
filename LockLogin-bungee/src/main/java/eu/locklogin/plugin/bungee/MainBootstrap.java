package eu.locklogin.plugin.bungee;

import com.google.gson.JsonObject;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.common.security.AllowedCommand;
import eu.locklogin.api.common.security.client.CommandProxy;
import eu.locklogin.api.common.session.online.SessionDataContainer;
import eu.locklogin.api.common.utils.Channel;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.common.utils.dependencies.DependencyManager;
import eu.locklogin.api.common.utils.dependencies.PluginDependency;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.common.web.STFetcher;
import eu.locklogin.api.module.LoadRule;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginStatusChangeEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.ActionBarSender;
import eu.locklogin.api.module.plugin.client.MessageSender;
import eu.locklogin.api.module.plugin.client.TitleSender;
import eu.locklogin.api.module.plugin.client.permission.PermissionObject;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.server.MessageQue;
import eu.locklogin.api.module.plugin.javamodule.server.TargetServer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.com.message.DataMessage;
import eu.locklogin.plugin.bungee.plugin.Manager;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static eu.locklogin.plugin.bungee.LockLogin.*;

public class MainBootstrap {

    private final Main loader;

    public MainBootstrap(final Plugin main) {
        loader = (Main) main;

        try {
            JarManager.changeField(CurrentPlatform.class, "current_appender", new BruteLoader((URLClassLoader) main.getClass().getClassLoader()));
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
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

        SimpleScheduler check_scheduler = createServerChecker();
        check_scheduler.start();

        console.send("&aUsing KarmaAPI version {0}, compiled at {1} for jdk {2}", KarmaAPI.getVersion(), KarmaAPI.getBuildDate(), KarmaAPI.getCompilerVersion());

        STFetcher fetcher = new STFetcher();
        fetcher.check();

        CurrentPlatform.setOnDataContainerUpdate(() -> {
            for (ServerInfo server : plugin.getProxy().getServers().values()) {
                Manager.sender.queue(server).insert(DataMessage.newInstance(DataType.LOGGED, Channel.PLUGIN, server.getPlayers().stream().findAny().orElse(null))
                        .addProperty("login_count", SessionDataContainer.getLogged()).getInstance().build());

                Manager.sender.queue(server).insert(DataMessage.newInstance(DataType.REGISTERED, Channel.PLUGIN, server.getPlayers().stream().findAny().orElse(null))
                        .addProperty("register_count", SessionDataContainer.getRegistered()).getInstance().build());
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

                    Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.SESSION, Channel.ACCOUNT, player)
                            .getInstance().build());

                    Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.PIN, Channel.ACCOUNT, player)
                            .addProperty("pin", false).getInstance().build());

                    Manager.sender.queue(BungeeSender.serverFromPlayer(player)).insert(DataMessage.newInstance(DataType.GAUTH, Channel.ACCOUNT, player)
                            .getInstance().build());

                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.API,
                            UserAuthenticateEvent.Result.SUCCESS,
                            modulePlayer,
                            "",
                            null);
                    ModulePlugin.callEvent(event);

                    user.checkServer(0, true);
                    user.send(event.getAuthMessage());
                }
            }
        };
        Consumer<ModulePlayer> onClose = modulePlayer -> {
            UUID id = modulePlayer.getUUID();

            ProxiedPlayer player = loader.getProxy().getPlayer(id);
            if (player != null) {
                User user = new User(player);

                String cmd = "account close";
                UUID cmd_id = CommandProxy.mask(cmd, "close");
                String exec = CommandProxy.getCommand(cmd_id);

                user.performCommand(exec);
            }
        };
        BiFunction<UUID, PermissionObject, Boolean> hasPermission = (id, permission) -> {
            ProxiedPlayer player = loader.getProxy().getPlayer(id);
            if (player != null) {
                switch (permission.getCriteria()) {
                    case TRUE:
                        return !player.hasPermission("!" + permission.getPermission());
                    case FALSE:
                    case OP:
                    default:
                        return player.hasPermission(permission.getPermission());
                }
            }

            return false;
        };
        Function<UUID, Boolean> opContainer = id -> {
            ProxiedPlayer player = loader.getProxy().getPlayer(id);
            if (player != null) {
                return player.hasPermission("*") || player.hasPermission("'*'");
            }

            return false;
        };

        Function<String, Set<ModulePlayer>> onPlayers = (name) -> {
            ServerInfo info = plugin.getProxy().getServerInfo(name);
            Set<ModulePlayer> players = new LinkedHashSet<>();

            if (info != null) {
                info.getPlayers().forEach((player) -> {
                    User user = new User(player);
                    players.add(user.getModule());
                });
            }

            return players;
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

        AllowedCommand.scan();
        Manager.initialize();

        SimpleScheduler scheduler = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true).multiThreading(true);
        scheduler.restartAction(() -> {
            for (TargetServer server : CurrentPlatform.getServer().getServers()) {
                MessageQue queue = fetchQueue(server);
                if (queue != null) {
                    JsonObject data = queue.previewMessage();

                    if (data != null) {
                        data = queue.nextMessage();

                        for (ModulePlayer player : server.getOnlinePlayers()) {
                            if (player.isPlaying()) {
                                if (data != null) {
                                    Manager.sender.queue(BungeeSender.serverFromPlayer(player.getPlayer()))
                                            .insert(DataMessage.newInstance(DataType.LISTENER, Channel.PLUGIN, player.getPlayer())
                                                    .addJson(data).getInstance().build());

                                    queue.nextMessage();
                                    plugin.console().send("Forwarding module message to server {0}", Level.INFO, server.getName());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }).start();
    }

    @NotNull
    private static SimpleScheduler createServerChecker() {
        SimpleScheduler check_scheduler = new SourceScheduler(plugin, 10, SchedulerUnit.SECOND, true);
        check_scheduler.restartAction(() -> {
            for (ServerInfo server : plugin.getProxy().getServers().values()) {
                if (!ServerDataStorage.needsProxyKnowledge(server.getName())) {
                    server.ping((result, error) -> {
                        if (error != null) {
                            ServerDataStorage.removeProxyRegistered(server.getName());
                            plugin.console().send("Failed to ping server {0}. Marking it as offline", Level.WARNING, server.getName());
                        }
                    });
                }
            }
        });
        return check_scheduler;
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
