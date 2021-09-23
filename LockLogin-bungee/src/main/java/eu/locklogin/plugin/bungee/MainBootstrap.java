package eu.locklogin.plugin.bungee;

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.common.security.AllowedCommand;
import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.common.utils.dependencies.DependencyManager;
import eu.locklogin.api.common.utils.dependencies.PluginDependency;
import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.common.web.STFetcher;
import eu.locklogin.api.module.LoadRule;
import eu.locklogin.api.module.plugin.api.channel.ModuleMessageService;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginStatusChangeEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.ActionBarSender;
import eu.locklogin.api.module.plugin.client.MessageSender;
import eu.locklogin.api.module.plugin.client.TitleSender;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.plugin.Manager;
import eu.locklogin.plugin.bungee.plugin.sender.DataSender;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.bungee.makeiteasy.TitleMessage;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.loader.KarmaBootstrap;
import ml.karmaconfigs.api.common.timer.SourceSecondsTimer;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static eu.locklogin.plugin.bungee.LockLogin.console;
import static eu.locklogin.plugin.bungee.LockLogin.plugin;
import static eu.locklogin.plugin.bungee.plugin.sender.DataSender.CHANNEL_PLAYER;
import static eu.locklogin.plugin.bungee.plugin.sender.DataSender.PLUGIN_CHANNEL;

public class MainBootstrap implements KarmaBootstrap {

    private final static File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));

    private final Main loader;
    private final DependencyManager manager = new DependencyManager(this);

    public MainBootstrap(final Plugin main) {
        loader = (Main) main;

        try {
            JarManager.changeField(CurrentPlatform.class, "current_appender", getAppender());
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void enable() {
        for (Dependency pluginDependency : Dependency.values()) {
            PluginDependency dependency = pluginDependency.getAsDependency();

            if (FileInfo.showChecksums(lockloginFile)) {
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
        manager.loadDependencies();

        console.send("&aUsing KarmaAPI version {0}, compiled at {1} for jdk {2}", KarmaAPI.getVersion(), KarmaAPI.getBuildDate(), KarmaAPI.getCompilerVersion());

        STFetcher fetcher = new STFetcher();
        fetcher.check();

        loader.getProxy().getScheduler().runAsync(loader, () -> {
            CurrentPlatform.setOnDataContainerUpdate(() -> {
                for (ServerInfo server : plugin.getProxy().getServers().values()) {
                    DataSender.send(server, DataSender.getBuilder(DataType.LOGGED, PLUGIN_CHANNEL, null).addIntData(SessionDataContainer.getLogged()).build());
                    DataSender.send(server, DataSender.getBuilder(DataType.REGISTERED, PLUGIN_CHANNEL, null).addIntData(SessionDataContainer.getRegistered()).build());
                }
            });

            console.getData().setOkPrefix("&aOk &e>> &7");
            console.getData().setInfoPrefix("&7Info &e>> &7");
            console.getData().setWarnPrefix("&6Warning &e>> &7");
            console.getData().setGravPrefix("&4Grave &e>> &7");

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
                    if (!StringUtils.isNullOrEmpty(messageSender.getMessage())) {
                        player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(StringUtils.toColor(messageSender.getMessage())));
                    }
                }
            };
            Consumer<TitleSender> onTitle = messageSender -> {
                ProxiedPlayer player = messageSender.getPlayer().getPlayer();

                if (player != null) {
                    if (StringUtils.isNullOrEmpty(messageSender.getTitle()) && StringUtils.isNullOrEmpty(messageSender.getSubtitle()))
                        return;

                    TitleMessage title = new TitleMessage(player, messageSender.getTitle(), messageSender.getSubtitle());
                    title.send(messageSender.getFadeOut(), messageSender.getKeepIn(), messageSender.getHideIn());
                }
            };
            Consumer<MessageSender> onKick = messageSender -> {
                SimpleScheduler scheduler = new SourceSecondsTimer(plugin, 1, false).multiThreading(false);
                scheduler.endAction(() -> scheduler.requestSync(() -> {
                    if (messageSender.getSender() instanceof ModulePlayer) {
                        ModulePlayer mp = (ModulePlayer) messageSender.getSender();
                        ProxiedPlayer player = mp.getPlayer();

                        if (player != null)
                            player.disconnect(TextComponent.fromLegacyText(StringUtils.toColor(messageSender.getMessage())));
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

                        DataSender.MessageData login = DataSender.getBuilder(DataType.SESSION, CHANNEL_PLAYER, player).build();
                        DataSender.MessageData pin = DataSender.getBuilder(DataType.PIN, CHANNEL_PLAYER, player).addTextData("close").build();
                        DataSender.MessageData gauth = DataSender.getBuilder(DataType.GAUTH, CHANNEL_PLAYER, player).build();

                        DataSender.send(player, login);
                        DataSender.send(player, pin);
                        DataSender.send(player, gauth);

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

            prepareManager();

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
        });
    }

    @Override
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
        getAppender().close();
    }

    @Override
    public KarmaSource getSource() {
        return loader;
    }

    /**
     * Prepare LockLoginManager module
     */
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
}
