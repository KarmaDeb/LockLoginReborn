package eu.locklogin.plugin.bungee;

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.common.injector.dependencies.DependencyManager;
import eu.locklogin.api.common.security.AllowedCommand;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.common.utils.dependencies.PluginDependency;
import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.common.web.STFetcher;
import eu.locklogin.api.module.LoadRule;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.api.channel.ModuleMessageService;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginStatusChangeEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.client.MessageSender;
import eu.locklogin.api.module.plugin.client.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleLoader;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleManager;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.plugin.Manager;
import eu.locklogin.plugin.bungee.plugin.sender.DataSender;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.loader.KarmaBootstrap;
import ml.karmaconfigs.api.common.utils.PrefixConsoleData;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static eu.locklogin.plugin.bungee.LockLogin.fromPlayer;
import static eu.locklogin.plugin.bungee.plugin.sender.DataSender.CHANNEL_PLAYER;

public class MainBootstrap implements KarmaBootstrap {

    private final static File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));

    private final Main loader;
    private final DependencyManager manager = new DependencyManager(this);

    public MainBootstrap(final Plugin main) {
        loader = (Main) main;

        ChecksumTables tables = new ChecksumTables();
        STFetcher fetcher = new STFetcher();
        tables.checkTables();
        fetcher.check();

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
        manager.loadDependencies();

        Console.send("&aInjected plugin KarmaAPI version {0}, compiled at {1} for jdk {2}", KarmaAPI.getVersion(), KarmaAPI.getBuildDate(), KarmaAPI.getCompilerVersion());

        loader.getProxy().getScheduler().runAsync(loader, () -> {
            PrefixConsoleData prefixData = new PrefixConsoleData(loader);
            prefixData.setOkPrefix("&aOk &e>> &7");
            prefixData.setInfoPrefix("&7Info &e>> &7");
            prefixData.setWarnPrefix("&6Warning &e>> &7");
            prefixData.setGravPrefix("&4Grave &e>> &7");

            Consumer<MessageSender> onMessage = messageSender -> {
                ModulePlayer modulePlayer = messageSender.getPlayer();
                UUID id = modulePlayer.getUUID();

                ProxiedPlayer client = loader.getProxy().getPlayer(id);
                if (client != null)
                    client.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messageSender.getMessage())));
            };
            Consumer<MessageSender> onKick = messageSender -> {
                ModulePlayer modulePlayer = messageSender.getPlayer();
                UUID id = modulePlayer.getUUID();

                ProxiedPlayer client = loader.getProxy().getPlayer(id);
                if (client != null)
                    client.disconnect(TextComponent.fromLegacyText(StringUtils.toColor(messageSender.getMessage())));
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

                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.API, UserAuthenticateEvent.Result.SUCCESS, fromPlayer(player), "", null);
                        JavaModuleManager.callEvent(event);

                        user.checkServer(0);
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
                JarManager.changeField(ModulePlayer.class, "onKick", onKick);
                JarManager.changeField(ModulePlayer.class, "onLogin", onLogin);
                JarManager.changeField(ModulePlayer.class, "onClose", onClose);
                JarManager.changeField(ModuleMessageService.class, "onDataSent", onDataSend);
            } catch (Throwable ignored) {}

            prepareManager();

            LockLogin.logger.scheduleLog(Level.OK, "LockLogin initialized and all its dependencies has been loaded");

            File[] moduleFiles = LockLogin.getLoader().getDataFolder().listFiles();
            Set<PluginModule> wait = new HashSet<>();
            if (moduleFiles != null) {
                for (File file : moduleFiles) {
                    PluginModule module = JavaModuleLoader.getByName(file.getName().replace(".jar", ""));
                    if (module != null) {
                        if (module.loadRule().equals(LoadRule.PREPLUGIN)) {
                            module.load();
                        } else {
                            wait.add(module);
                        }
                    }
                }
            }

            PluginStatusChangeEvent event = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.LOAD, null);
            JavaModuleManager.callEvent(event);

            AllowedCommand.scan();

            Manager.initialize(wait);
        });
    }

    @Override
    public void disable() {
        PluginStatusChangeEvent event = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UNLOAD, null);
        JavaModuleManager.callEvent(event);

        File[] moduleFiles = LockLogin.getLoader().getDataFolder().listFiles();
        if (moduleFiles != null) {
            for (File module : moduleFiles) {
                LockLogin.getLoader().unloadModule(module.getName());
            }
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
