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
import eu.locklogin.api.module.plugin.client.ActionBarSender;
import eu.locklogin.api.module.plugin.client.MessageSender;
import eu.locklogin.api.module.plugin.client.ModulePlayer;
import eu.locklogin.api.module.plugin.client.TitleSender;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.Platform;
import eu.locklogin.plugin.velocity.plugin.Manager;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.loader.JarAppender;
import ml.karmaconfigs.api.common.karma.loader.KarmaBootstrap;
import ml.karmaconfigs.api.common.timer.SourceSecondsTimer;
import ml.karmaconfigs.api.common.utils.PrefixConsoleData;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.velocity.makeiteasy.TitleMessage;
import net.kyori.adventure.text.Component;
import org.bstats.velocity.Metrics;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Plugin(id = "locklogin", name = "LockLogin", version = "1.12.31", authors = {"KarmaDev"}, description = "LockLogin is an advanced login plugin, one of the most secure available, with tons of features. It has a lot of customization options to not say almost everything is customizable. Regular updates and one of the bests discord supports ( according to spigotmc reviews ). LockLogin is a plugin always open to new feature requests, and bug reports. More than a plugin, a plugin you can contribute indirectly; A community plugin for the plugin community.", url = "https://locklogin.eu/")
public class Main implements KarmaBootstrap, KarmaSource {

    private static final File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));

    static ProxyServer server;
    static PluginContainer container;
    static Metrics.Factory factory;
    static KarmaSource source;

    private static Main instance;
    private final JarAppender appender;

    @Inject
    public Main(ProxyServer server, Metrics.Factory fact) {
        CurrentPlatform.setPlatform(Platform.VELOCITY);
        CurrentPlatform.setMain(Main.class);
        CurrentPlatform.setOnline(server.getConfiguration().isOnlineMode());

        Main.server = server;
        factory = fact;
        appender = new VelocitySubJarAppender(this);
        source = this;

        ChecksumTables tables = new ChecksumTables();
        tables.checkTables();

        try {
            JarManager.changeField(CurrentPlatform.class, "current_appender", getAppender());
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public static Main get() {
        return instance;
    }

    @Override
    public void enable() {
        new SourceSecondsTimer(this, 1, false).multiThreading(true).endAction(() -> {
            Main.instance = Main.this;
            Console.send("&aInjected plugin KarmaAPI version {0}, compiled at {1} for jdk {2}", KarmaAPI.getVersion(), KarmaAPI.getBuildDate(), KarmaAPI.getCompilerVersion());
            Optional<PluginContainer> container = Main.server.getPluginManager().getPlugin("locklogin");

            if (container.isPresent()) {
                for (Dependency pluginDependency : Dependency.values()) {
                    PluginDependency dependency = pluginDependency.getAsDependency();

                    if (FileInfo.showChecksums(Main.lockloginFile)) {
                        System.out.println("Current checksum for " + dependency.getName());
                        System.out.println("Adler32: " + dependency.getAdlerCheck());
                        System.out.println("CRC32: " + dependency.getCRCCheck());
                        System.out.println("Fetched checksum for " + dependency.getName());
                        System.out.println("Adler32: " + ChecksumTables.getAdler(dependency));
                        System.out.println("CRC32: " + ChecksumTables.getCRC(dependency));
                    }

                    JarManager manager = new JarManager(dependency);
                    manager.process(false);
                }
                JarManager.downloadAll();

                Main.container = container.get();

                STFetcher fetcher = new STFetcher();
                fetcher.check();

                CurrentPlatform.setOnDataContainerUpdate(() -> {
                    for (RegisteredServer server : server.getAllServers()) {
                        DataSender.send(server, DataSender.getBuilder(DataType.LOGGED, DataSender.PLUGIN_CHANNEL, null).addIntData(SessionDataContainer.getLogged()).build());
                        DataSender.send(server, DataSender.getBuilder(DataType.REGISTERED, DataSender.PLUGIN_CHANNEL, null).addIntData(SessionDataContainer.getRegistered()).build());
                    }
                });

                PrefixConsoleData prefixData = new PrefixConsoleData(Main.source);
                prefixData.setOkPrefix("&aOk &e>> &7");
                prefixData.setInfoPrefix("&7Info &e>> &7");
                prefixData.setWarnPrefix("&6Warning &e>> &7");
                prefixData.setGravPrefix("&4Grave &e>> &7");

                Consumer<MessageSender> onMessage = messageSender -> {
                    Player player = messageSender.getPlayer().getPlayer();

                    if (player != null) {
                        player.sendMessage(Component.text().content(StringUtils.toColor(messageSender.getMessage())).build());
                    }
                };
                Consumer<ActionBarSender> onActionBar = messageSender -> {
                    Player player = messageSender.getPlayer().getPlayer();

                    if (player != null) {
                        player.sendActionBar(Component.text().content(StringUtils.toColor(messageSender.getMessage())).build());
                    }
                };
                Consumer<TitleSender> onTitle = messageSender -> {
                    Player player = messageSender.getPlayer().getPlayer();

                    if (player != null) {
                        TitleMessage title = new TitleMessage(player, messageSender.getTitle(), messageSender.getSubtitle());
                        title.send(messageSender.getFadeOut(), messageSender.getKeepIn(), messageSender.getHideIn());
                    }
                };
                Consumer<MessageSender> onKick = messageSender -> {
                    Player player = messageSender.getPlayer().getPlayer();

                    if (player != null) {
                        player.disconnect(Component.text().content(StringUtils.toColor(messageSender.getMessage())).build());
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

                            UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.API, UserAuthenticateEvent.Result.SUCCESS, LockLogin.fromPlayer(player), "", null);
                            ModulePlugin.callEvent(event);

                            user.checkServer(0);
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
                BiConsumer<String, byte[]> onDataSend = DataSender::sendModule;

                try {
                    JarManager.changeField(ModulePlayer.class, "onChat", onMessage);
                    JarManager.changeField(ModulePlayer.class, "onBar", onActionBar);
                    JarManager.changeField(ModulePlayer.class, "onTitle", onTitle);
                    JarManager.changeField(ModulePlayer.class, "onKick", onKick);
                    JarManager.changeField(ModulePlayer.class, "onLogin", onLogin);
                    JarManager.changeField(ModulePlayer.class, "onClose", onClose);
                    JarManager.changeField(ModuleMessageService.class, "onDataSent", onDataSend);
                } catch (Throwable ignored) {}

                Main.this.prepareManager();

                LockLogin.logger.scheduleLog(Level.OK, "LockLogin initialized and all its dependencies has been loaded");

                File[] moduleFiles = LockLogin.getLoader().getDataFolder().listFiles();
                if (moduleFiles != null) {
                    List<File> files = Arrays.asList(moduleFiles);
                    Iterator<File> iterator = files.iterator();
                    do {
                        File file = iterator.next();
                        if (file.isFile()) {
                            LockLogin.getLoader().loadModule(file, LoadRule.PREPLUGIN);
                        }
                    } while (iterator.hasNext());
                }

                PluginStatusChangeEvent event = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.LOAD, null);
                ModulePlugin.callEvent(event);

                AllowedCommand.scan();

                Manager.initialize();
            } else {
                Main.server.getConsoleCommandSource().sendMessage(Component.text().content(StringUtils.toColor("&cTried to load LockLogin but is not even loaded by velocity!")).build());
            }
        }).start();
    }

    @Override
    public void disable() {
        PluginStatusChangeEvent event = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UNLOAD, null);
        ModulePlugin.callEvent(event);
        File[] moduleFiles = LockLogin.getLoader().getDataFolder().listFiles();
        if (moduleFiles != null)
            for (File module : moduleFiles)
                LockLogin.getLoader().unloadModule(module);

        Manager.terminate();
        stopTasks();
    }

    @Override
    public JarAppender getAppender() {
        return appender;
    }

    @Override
    public KarmaSource getSource() {
        return this;
    }

    private void prepareManager() {
        PluginDependency dependency = Dependency.MANAGER.getAsDependency();

        if (FileInfo.showChecksums(lockloginFile)) {
            System.out.println("Current checksum for " + dependency.getName());
            System.out.println("Adler32: " + dependency.getAdlerCheck());
            System.out.println("CRC32: " + dependency.getCRCCheck());
            System.out.println("Fetched checksum for " + dependency.getName());
            System.out.println("Adler32: " + ChecksumTables.getAdler(dependency));
            System.out.println("CRC32: " + ChecksumTables.getCRC(dependency));
        }

        JarManager manager = new JarManager(dependency);
        manager.process(true);

        JarManager.downloadAll();
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
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        switch (config.getUpdaterOptions().getChannel()) {
            case SNAPSHOT:
                return "https://locklogin.eu/version/snapshot.kupdter";
            case RC:
                return "https://locklogin.eu/version/candidate.kupdter";
            case RELEASE:
            default:
                return "https://locklogin.eu/version/release.kupdter";
        }
    }
}