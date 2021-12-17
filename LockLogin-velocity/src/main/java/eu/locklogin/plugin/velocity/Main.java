package eu.locklogin.plugin.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.common.security.AllowedCommand;
import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.common.utils.dependencies.PluginDependency;
import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.common.web.STFetcher;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.LoadRule;
import eu.locklogin.api.module.plugin.api.channel.ModuleMessageService;
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
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.Platform;
import eu.locklogin.plugin.velocity.plugin.Manager;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.player.User;
import eu.locklogin.plugin.velocity.util.scheduler.VelocitySyncScheduler;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.loader.BruteLoader;
import ml.karmaconfigs.api.common.timer.SourceSecondsTimer;
import ml.karmaconfigs.api.common.timer.scheduler.Scheduler;
import ml.karmaconfigs.api.common.utils.URLUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.velocity.makeiteasy.TitleMessage;
import net.kyori.adventure.text.Component;
import org.bstats.velocity.Metrics;

import java.io.File;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Plugin(
        id = "locklogin",
        name = "LockLogin",
        version = "1.13.9",
        authors = {"KarmaDev"},
        description =
                "LockLogin is an advanced login plugin, one of the most secure available, with tons of features. " +
                "It has a lot of customization options to not say almost everything is customizable. Regular updates " +
                "and one of the bests discord supports ( according to spigotmc reviews ). LockLogin is a plugin " +
                "always open to new feature requests, and bug reports. More than a plugin, a plugin you can contribute indirectly; " +
                "A community plugin for the plugin community.",
        url = "https://locklogin.eu/",
        dependencies = {
                @com.velocitypowered.api.plugin.Dependency(id = "anotherbarelycodedkarmaplugin")
        })
public class Main implements KarmaSource {

    private static final File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));
    static ProxyServer server;
    static PluginContainer container;
    static Metrics.Factory factory;
    static KarmaSource source;
    private static Console console;
    private static Scheduler async;
    private static Scheduler sync;
    private static Main instance;
    private final BruteLoader appender;

    @Inject
    public Main(ProxyServer server, Metrics.Factory fact) {
        APISource.addProvider(this);

        console = new Console(this, (message) -> server.getConsoleCommandSource().sendMessage(
                Component.text().content(StringUtils.toColor(message)).build()));

        CurrentPlatform.setPlatform(Platform.VELOCITY);
        CurrentPlatform.setMain(Main.class);
        CurrentPlatform.setOnline(server.getConfiguration().isOnlineMode());

        Main.server = server;
        factory = fact;
        appender = new BruteLoader((URLClassLoader) Main.class.getClassLoader());
        source = this;

        ChecksumTables tables = new ChecksumTables();
        tables.checkTables();

        try {
            JarManager.changeField(CurrentPlatform.class, "current_appender", appender);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public static Main get() {
        return instance;
    }

    public void enable() {
        new SourceSecondsTimer(this, 1, false).multiThreading(true).endAction(() -> {
            Main.instance = Main.this;
            Console console = new Console(source);

            console.send("&aInjected plugin KarmaAPI version {0}, compiled at {1} for jdk {2}", KarmaAPI.getVersion(), KarmaAPI.getBuildDate(), KarmaAPI.getCompilerVersion());
            Optional<PluginContainer> container = Main.server.getPluginManager().getPlugin("locklogin");

            if (container.isPresent()) {
                Main.container = container.get();

                async = new VelocitySyncScheduler();
                sync = new VelocitySyncScheduler();

                for (Dependency pluginDependency : Dependency.values()) {
                    PluginDependency dependency = pluginDependency.getAsDependency();

                    if (FileInfo.showChecksums(Main.lockloginFile)) {
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
                }
                JarManager.downloadAll();

                STFetcher fetcher = new STFetcher();
                fetcher.check();

                CurrentPlatform.setOnDataContainerUpdate(() -> {
                    for (RegisteredServer server : server.getAllServers()) {
                        DataSender.send(server, DataSender.getBuilder(DataType.LOGGED, DataSender.PLUGIN_CHANNEL, null).addIntData(SessionDataContainer.getLogged()).build());
                        DataSender.send(server, DataSender.getBuilder(DataType.REGISTERED, DataSender.PLUGIN_CHANNEL, null).addIntData(SessionDataContainer.getRegistered()).build());
                    }
                });

                console.getData().setOkPrefix("&aOk &e>> &7");
                console.getData().setInfoPrefix("&7Info &e>> &7");
                console.getData().setWarnPrefix("&6Warning &e>> &7");
                console.getData().setGravePrefix("&4Grave &e>> &7");

                Consumer<MessageSender> onMessage = messageSender -> {
                    if (messageSender.getSender() instanceof ModulePlayer) {
                        ModulePlayer mp = (ModulePlayer) messageSender.getSender();
                        Player player = mp.getPlayer();

                        if (player != null) {
                            User user = new User(player);
                            user.send(messageSender.getMessage());
                        }
                    }
                };
                Consumer<ActionBarSender> onActionBar = messageSender -> {
                    Player player = messageSender.getPlayer().getPlayer();

                    if (player != null) {
                        if (!StringUtils.isNullOrEmpty(messageSender.getMessage())) {
                            player.sendActionBar(Component.text().content(StringUtils.toColor(messageSender.getMessage())).build());
                        }
                    }
                };
                Consumer<TitleSender> onTitle = messageSender -> {
                    Player player = messageSender.getPlayer().getPlayer();

                    if (player != null) {
                        if (StringUtils.isNullOrEmpty(messageSender.getTitle()) && StringUtils.isNullOrEmpty(messageSender.getSubtitle()))
                            return;

                        TitleMessage title = new TitleMessage(player, messageSender.getTitle(), messageSender.getSubtitle());
                        title.send(messageSender.getFadeOut(), messageSender.getKeepIn(), messageSender.getHideIn());
                    }
                };
                Consumer<MessageSender> onKick = messageSender -> {
                    if (messageSender.getSender() instanceof ModulePlayer) {
                        ModulePlayer mp = (ModulePlayer) messageSender.getSender();
                        Player player = mp.getPlayer();

                        if (player != null) {
                            player.disconnect(Component.text().content(StringUtils.toColor(messageSender.getMessage())).build());
                        }
                    }
                };
                Consumer<ModulePlayer> onLogin = modulePlayer -> {
                    Player player = modulePlayer.getPlayer();

                    if (player != null) {
                        User user = new User(player);
                        ClientSession session = user.getSession();

                        if (!session.isLogged() || !session.isTempLogged()) {
                            session.setCaptchaLogged(true);
                            session.setLogged(true);
                            session.setPinLogged(true);
                            session.set2FALogged(true);

                            DataSender.MessageData login = DataSender.getBuilder(DataType.SESSION, "ll:account", player).build();
                            DataSender.MessageData pin = DataSender.getBuilder(DataType.PIN, "ll:account", player).addTextData("close").build();
                            DataSender.MessageData gauth = DataSender.getBuilder(DataType.GAUTH, "ll:account", player).build();

                            DataSender.send(player, login);
                            DataSender.send(player, pin);
                            DataSender.send(player, gauth);

                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.API,
                                    UserAuthenticateEvent.Result.SUCCESS,
                                    user.getModule(),
                                    "",
                                    null);
                            ModulePlugin.callEvent(event);

                            user.checkServer(0);
                            user.send(event.getAuthMessage());
                        }
                    }
                };
                Consumer<ModulePlayer> onClose = modulePlayer -> {
                    Player player = modulePlayer.getPlayer();

                    if (player != null) {
                        User user = new User(player);
                        user.performCommand("account close");
                    }
                };
                Consumer<PermissionContainer> hasPermission = permContainer -> {
                    UUID id = permContainer.getAttachment().getUUID();

                    server.getPlayer(id).ifPresent((player -> {
                        PermissionObject permission = permContainer.getPermission();

                        switch (permission.getCriteria()) {
                            case TRUE:
                                permContainer.setResult(!player.hasPermission("!" + permission.getPermission()));
                                break;
                            case FALSE:
                            case OP:
                            default:
                                permContainer.setResult(player.hasPermission(permission.getPermission()));
                        }
                    }));
                };
                Consumer<OpContainer> opContainer = pContainer -> {
                    UUID id = pContainer.getAttachment().getUUID();

                    server.getPlayer(id).ifPresent((player) -> pContainer.setResult(player.hasPermission("*") || player.hasPermission("'*'")));
                };
                BiConsumer<String, byte[]> onDataSend = DataSender::sendModule;

                try {
                    JarManager.changeField(ModulePlayer.class, "onChat", onMessage);
                    JarManager.changeField(ModulePlayer.class, "onBar", onActionBar);
                    JarManager.changeField(ModulePlayer.class, "onTitle", onTitle);
                    JarManager.changeField(ModulePlayer.class, "onKick", onKick);
                    JarManager.changeField(ModulePlayer.class, "onLogin", onLogin);
                    JarManager.changeField(ModulePlayer.class, "onClose", onClose);
                    JarManager.changeField(ModulePlayer.class, "hasPermission", hasPermission);
                    JarManager.changeField(ModulePlayer.class, "opContainer", opContainer);

                    JarManager.changeField(ModuleMessageService.class, "onDataSent", onDataSend);
                } catch (Throwable ignored) {
                }

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

                AllowedCommand.scan();

                Manager.initialize();

                if (moduleFiles != null) {
                    List<File> files = Arrays.asList(moduleFiles);
                    Iterator<File> iterator = files.iterator();
                    do {
                        File file = iterator.next();
                        LockLogin.getLoader().loadModule(file, LoadRule.POSTPLUGIN);
                    } while (iterator.hasNext());
                }
            } else {
                Main.server.getConsoleCommandSource().sendMessage(Component.text().content(StringUtils.toColor("&cTried to load LockLogin but is not even loaded by velocity!")).build());
            }
        }).start();
    }

    public void disable() {
        Event event = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UNLOAD, null);
        ModulePlugin.callEvent(event);
        File[] moduleFiles = LockLogin.getLoader().getDataFolder().listFiles();
        if (moduleFiles != null)
            for (File module : moduleFiles)
                LockLogin.getLoader().unloadModule(module);

        Manager.terminate();
        stopTasks();
    }

    @Subscribe(order = PostOrder.LAST)
    public void onProxyInitialization(ProxyInitializeEvent e) {
        enable();
    }

    @Subscribe(order = PostOrder.LAST)
    public void onProxyInitialization(ProxyShutdownEvent e) {
        disable();
    }

    public String name() {
        return FileInfo.getJarName(lockloginFile);
    }

    public String version() {
        return FileInfo.getJarVersion(lockloginFile);
    }

    public String description() {
        return FileInfo.getJarDescription(lockloginFile);
    }

    public String[] authors() {
        String authors = FileInfo.getJarAuthors(lockloginFile);
        if (authors.contains(",")) {
            return authors.replaceAll("\\s", "").split(",");
        } else {
            return new String[]{authors};
        }
    }

    @Override
    public String updateURL() {
        String host = "https://karmarepo.000webhostapp.com/locklogin/version/";
        if (!URLUtils.exists(host)) {
            host = "https://karmaconfigs.github.io/updates/LockLogin/version/";
        }

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        if (config != null) {
            switch (config.getUpdaterOptions().getChannel()) {
                case SNAPSHOT:
                    return host + "snapshot.kupdter";
                case RC:
                    return host + "candidate.kupdter";
                case RELEASE:
                default:
                    return host + "release.kupdter";
            }
        }

        return host + "release.kupdter";
    }

    @Override
    public Console console() {
        return console;
    }

    @Override
    public Scheduler async() {
        return async;
    }

    @Override
    public Scheduler sync() {
        return sync;
    }
}