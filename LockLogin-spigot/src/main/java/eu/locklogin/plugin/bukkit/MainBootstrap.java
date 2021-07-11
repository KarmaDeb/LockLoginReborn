package eu.locklogin.plugin.bukkit;

import eu.locklogin.api.common.injector.dependencies.DependencyManager;
import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.common.web.STFetcher;
import eu.locklogin.api.module.LoadRule;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.plugin.bukkit.plugin.Manager;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.loader.KarmaBootstrap;
import ml.karmaconfigs.api.common.utils.PrefixConsoleData;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import eu.locklogin.api.module.plugin.api.channel.ModuleMessageService;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginStatusChangeEvent;
import eu.locklogin.api.module.plugin.client.MessageSender;
import eu.locklogin.api.module.plugin.client.ModulePlayer;
import eu.locklogin.api.common.utils.dependencies.PluginDependency;
import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.plugin.bungee.BungeeSender;
import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.common.security.AllowedCommand;
import eu.locklogin.api.common.utils.FileInfo;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

        STFetcher fetcher = new STFetcher();
        fetcher.check();

        loader.getServer().getScheduler().runTaskAsynchronously(loader, () -> {
            PrefixConsoleData prefixData = new PrefixConsoleData(loader);
            prefixData.setOkPrefix("&aOk &e>> &7");
            prefixData.setInfoPrefix("&7Info &e>> &7");
            prefixData.setWarnPrefix("&6Warning &e>> &7");
            prefixData.setGravPrefix("&4Grave &e>> &7");

            Consumer<MessageSender> onMessage = messageSender -> {
                ModulePlayer modulePlayer = messageSender.getPlayer();
                UUID id = modulePlayer.getUUID();

                Player client = loader.getServer().getPlayer(id);
                if (client != null)
                    client.sendMessage(StringUtils.toColor(messageSender.getMessage()));
            };
            Consumer<MessageSender> onKick = messageSender -> {
                ModulePlayer modulePlayer = messageSender.getPlayer();
                UUID id = modulePlayer.getUUID();

                Player client = loader.getServer().getPlayer(id);
                if (client != null)
                    client.kickPlayer(StringUtils.toColor(messageSender.getMessage()));
            };
            BiConsumer<String, byte[]> onDataSend = BungeeSender::sendModule;

            try {
                JarManager.changeField(ModulePlayer.class, "onChat", onMessage);
                JarManager.changeField(ModulePlayer.class, "onKick", onKick);
                JarManager.changeField(ModuleMessageService.class, "onDataSent", onDataSend);
            } catch (Throwable ignored) {
            }

            prepareManager();

            LockLogin.logger.scheduleLog(Level.OK, "LockLogin initialized and all its dependencies has been loaded");

            File[] moduleFiles = LockLogin.getLoader().getDataFolder().listFiles();
            Set<PluginModule> wait = new HashSet<>();
            if (moduleFiles != null) {
                for (File file : moduleFiles) {
                    PluginModule module = ModuleLoader.getByName(file.getName().replace(".jar", ""));
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
            ModulePlugin.callEvent(event);

            AllowedCommand.scan();

            Manager.initialize(wait);
        });
    }

    @Override
    public void disable() {
        PluginStatusChangeEvent event = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UNLOAD, null);
        ModulePlugin.callEvent(event);

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
