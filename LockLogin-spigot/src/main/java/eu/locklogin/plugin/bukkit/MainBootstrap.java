package eu.locklogin.plugin.bukkit;

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.common.security.AllowedCommand;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.common.utils.dependencies.DependencyManager;
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
import eu.locklogin.api.module.plugin.client.TitleSender;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.plugin.Manager;
import eu.locklogin.plugin.bukkit.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bukkit.util.player.ClientVisor;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.bukkit.reflections.BarMessage;
import ml.karmaconfigs.api.bukkit.reflections.TitleMessage;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.loader.KarmaBootstrap;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static eu.locklogin.plugin.bukkit.LockLogin.console;

public class MainBootstrap implements KarmaBootstrap {

    private final static File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));

    private final Main loader;
    private final DependencyManager manager = new DependencyManager(this);

    public MainBootstrap(final JavaPlugin main) {

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

        console.send("&aInjected plugin KarmaAPI version {0}, compiled at {1} for jdk {2}", KarmaAPI.getVersion(), KarmaAPI.getBuildDate(), KarmaAPI.getCompilerVersion());

        STFetcher fetcher = new STFetcher();
        fetcher.check();

        console.getData().setOkPrefix("&aOk &e>> &7");
        console.getData().setInfoPrefix("&7Info &e>> &7");
        console.getData().setWarnPrefix("&6Warning &e>> &7");
        console.getData().setGravPrefix("&4Grave &e>> &7");

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
                    BarMessage bar = new BarMessage(player, messageSender.getMessage());
                    bar.send(false);
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

                User user = new User(player);
                user.kick(messageSender.getMessage());
            }
        };
        Consumer<ModulePlayer> onLogin = modulePlayer -> {
            UUID id = modulePlayer.getUUID();

            PluginConfiguration config = CurrentPlatform.getConfiguration();
            Player player = loader.getServer().getPlayer(id);
            if (player != null) {
                User user = new User(player);
                ClientSession session = user.getSession();

                if (!session.isLogged() || !session.isTempLogged()) {
                    session.setCaptchaLogged(true);
                    session.setLogged(true);
                    session.setPinLogged(true);
                    session.set2FALogged(true);

                    if (config.hideNonLogged()) {
                        ClientVisor visor = new ClientVisor(player);
                        visor.show();
                    }

                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.API,
                            UserAuthenticateEvent.Result.SUCCESS,
                            modulePlayer,
                            "",
                            null);
                    ModulePlugin.callEvent(event);

                    user.send(event.getAuthMessage());
                }
            }
        };
        Consumer<ModulePlayer> onClose = modulePlayer -> {
            UUID id = modulePlayer.getUUID();

            Player player = loader.getServer().getPlayer(id);
            if (player != null) {
                player.performCommand("account close");
            }
        };
        BiConsumer<String, byte[]> onDataSend = BungeeSender::sendModule;

        try {
            JarManager.changeField(ModulePlayer.class, "onChat", onMessage);
            JarManager.changeField(ModulePlayer.class, "onBar", onActionBar);
            JarManager.changeField(ModulePlayer.class, "onTitle", onTitle);
            JarManager.changeField(ModulePlayer.class, "onKick", onKick);
            JarManager.changeField(ModulePlayer.class, "onLogin", onLogin);
            JarManager.changeField(ModulePlayer.class, "onClose", onClose);
            JarManager.changeField(ModuleMessageService.class, "onDataSent", onDataSend);
        } catch (Throwable ignored) {
        }

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
