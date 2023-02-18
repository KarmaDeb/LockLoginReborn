package eu.locklogin.plugin.bukkit;

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.common.security.AllowedCommand;
import eu.locklogin.api.common.security.client.CommandProxy;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.common.utils.dependencies.DependencyManager;
import eu.locklogin.api.common.utils.dependencies.PluginDependency;
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
import eu.locklogin.api.plugin.PluginLicenseProvider;
import eu.locklogin.api.plugin.license.License;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.plugin.Manager;
import eu.locklogin.plugin.bukkit.plugin.bungee.BungeeSender;
import eu.locklogin.plugin.bukkit.util.files.data.LastLocation;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.bukkit.server.BukkitServer;
import ml.karmaconfigs.api.bukkit.server.Version;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.file.KarmaMain;
import ml.karmaconfigs.api.common.karma.loader.BruteLoader;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static eu.locklogin.plugin.bukkit.LockLogin.console;
import static eu.locklogin.plugin.bukkit.LockLogin.plugin;

public class MainBootstrap {

    private final Main loader;

    public MainBootstrap(final JavaPlugin main) {

        loader = (Main) main;

        try {
            JarManager.changeField(CurrentPlatform.class, "current_appender", new BruteLoader((URLClassLoader) main.getClass().getClassLoader()));
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public void enable() {
        for (Dependency pluginDependency : Dependency.values()) {
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
            if (pluginDependency == Dependency.APACHE_COMMONS) {
                if (BukkitServer.isUnder(Version.v1_8)) {
                    manager.process(false);
                    console.send("Apache Commons will be downloaded because your server version is too low. Please consider updating to at least 1.8", Level.GRAVE);
                }
            } else {
                manager.process(false);
                if (dependency.isHighPriority()) {
                    manager.downloadAndInject();
                }
            }
        }

        JarManager.downloadAll();
        DependencyManager.loadDependencies();

        //console.send("&aInjected plugin KarmaAPI version {0}, compiled at {1} for jdk {2}", KarmaAPI.getVersion(), KarmaAPI.getBuildDate(), KarmaAPI.getCompilerVersion());

        STFetcher fetcher = new STFetcher();
        fetcher.check();

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
            Player player = messageSender.getPlayer().getPlayer();

            if (player != null) {
                User user = new User(player);

                if (StringUtils.isNullOrEmpty(messageSender.getTitle()) && StringUtils.isNullOrEmpty(messageSender.getSubtitle()))
                    user.send("", "", 0, 0, 0);

                user.send(messageSender.getTitle(), messageSender.getSubtitle(), messageSender.getFadeOut(), messageSender.getKeepIn(), messageSender.getHideIn());
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

            Player player = loader.getServer().getPlayer(id);
            if (player != null) {
                User user = new User(player);
                ClientSession session = user.getSession();

                if (!session.isLogged() || !session.isTempLogged()) {
                    session.setCaptchaLogged(true);
                    session.setLogged(true);
                    session.setPinLogged(true);
                    session.set2FALogged(true);

                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.API,
                            UserAuthenticateEvent.Result.SUCCESS,
                            modulePlayer,
                            "",
                            null);
                    ModulePlugin.callEvent(event);

                    user.send(event.getAuthMessage());

                    if (CurrentPlatform.getConfiguration().takeBack()) {
                        LastLocation last = new LastLocation(player);
                        last.teleport();
                    }
                }
            }
        };
        Consumer<ModulePlayer> onClose = modulePlayer -> {
            UUID id = modulePlayer.getUUID();

            Player player = loader.getServer().getPlayer(id);
            if (player != null) {
                String cmd = "account close";
                UUID cmd_id = CommandProxy.mask(cmd, "close");
                String exec = CommandProxy.getCommand(cmd_id);

                player.performCommand(exec + cmd_id + " ");
            }
        };
        Consumer<PermissionContainer> hasPermission = container -> {
            UUID id = container.getAttachment().getUUID();

            Player player = loader.getServer().getPlayer(id);
            if (player != null) {
                PermissionObject permission = container.getPermission();
                Permission tmp;

                switch (permission.getCriteria()) {
                    case TRUE:
                        tmp = new Permission(permission.getPermission(), PermissionDefault.TRUE);
                        break;
                    case FALSE:
                        tmp = new Permission(permission.getPermission(), PermissionDefault.FALSE);
                        break;
                    case OP:
                    default:
                        tmp = new Permission(permission.getPermission(), PermissionDefault.OP);
                }

                container.setResult(player.hasPermission(tmp));
            }
        };
        Consumer<OpContainer> opContainer = container -> {
            UUID id = container.getAttachment().getUUID();

            Player player = loader.getServer().getPlayer(id);
            if (player != null) {
                container.setResult(player.isOp() || player.hasPermission("*") || player.hasPermission("'*'"));
            }
        };
        @SuppressWarnings("unused")
        BiConsumer<String, byte[]> onDataSend = BungeeSender::sendModule; //I may use this in a future

        try {
            JarManager.changeField(ModulePlayer.class, "onChat", onMessage);
            JarManager.changeField(ModulePlayer.class, "onBar", onActionBar);
            JarManager.changeField(ModulePlayer.class, "onTitle", onTitle);
            JarManager.changeField(ModulePlayer.class, "onKick", onKick);
            JarManager.changeField(ModulePlayer.class, "onLogin", onLogin);
            JarManager.changeField(ModulePlayer.class, "onClose", onClose);
            JarManager.changeField(ModulePlayer.class, "hasPermission", hasPermission);
            JarManager.changeField(ModulePlayer.class, "opContainer", opContainer);

            //JarManager.changeField(ModuleMessageService.class, "onDataSent", onDataSend);
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

        if (moduleFiles != null) {
            List<File> files = Arrays.asList(moduleFiles);
            Iterator<File> iterator = files.iterator();
            do {
                File file = iterator.next();
                LockLogin.getLoader().loadModule(file, LoadRule.POSTPLUGIN);
            } while (iterator.hasNext());
        }

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
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            } else {
                plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                    License installed = CurrentPlatform.getLicense();
                    if (installed == null) {
                        PluginConfiguration config = CurrentPlatform.getConfiguration();
                        if (config != null && config.isBungeeCord()) {
                            plugin.console().send("IMPORTANT! Please synchronize this server with your proxy license (/locklogin sync) or install a license (/locklogin install) and synchronize the installed license with your network to enable LockLogin BungeeCord", Level.GRAVE);
                        }
                    }
                }, 0, 20 * 30);
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
        }

        AllowedCommand.scan();
        Manager.initialize();
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
