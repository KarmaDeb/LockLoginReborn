package ml.karmaconfigs.locklogin.plugin.bukkit;

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

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.JarInjector;
import ml.karmaconfigs.api.common.KarmaPlugin;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.modules.api.channel.ModuleMessageService;
import ml.karmaconfigs.locklogin.api.modules.api.event.plugin.PluginStatusChangeEvent;
import ml.karmaconfigs.locklogin.api.modules.util.client.MessageSender;
import ml.karmaconfigs.locklogin.api.modules.util.client.ModulePlayer;
import ml.karmaconfigs.locklogin.api.modules.util.dependencies.Dependency;
import ml.karmaconfigs.locklogin.api.modules.util.dependencies.LockLoginDependencies;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleLoader;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.api.utils.platform.Platform;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.Manager;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee.BungeeSender;
import ml.karmaconfigs.locklogin.plugin.common.JarManager;
import ml.karmaconfigs.locklogin.plugin.common.security.AllowedCommand;
import ml.karmaconfigs.locklogin.plugin.common.utils.FileInfo;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.Messages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@KarmaPlugin
public final class Main extends JavaPlugin {

    private final static File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));

    private static boolean injected = false;

    @Override
    public void onEnable() {
        injected = injectAPI();

        if (injected) {
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                CurrentPlatform.setPlatform(Platform.BUKKIT);
                CurrentPlatform.setMain(Main.class);
                CurrentPlatform.setOnline(getServer().getOnlineMode());

                Consumer<MessageSender> onMessage = messageSender -> {
                    ModulePlayer modulePlayer = messageSender.getPlayer();
                    UUID id = modulePlayer.getUUID();

                    Player client = getServer().getPlayer(id);
                    if (client != null)
                        client.sendMessage(StringUtils.toColor(messageSender.getMessage()));
                };
                BiConsumer<String, byte[]> onDataSend = BungeeSender::sendModule;

                try {
                    JarManager.changeField(ModulePlayer.class, "onChat", true, onMessage);
                    JarManager.changeField(ModuleMessageService.class, "onDataSent", true, onDataSend);
                } catch (Throwable ignored) {}

                Console.setOkPrefix(this, "&aOk &e>> &7");
                Console.setInfoPrefix(this, "&7Info &e>> &7");
                Console.setWarningPrefix(this, "&6Warning &e>> &7");
                Console.setGravePrefix(this, "&4Grave &e>> &7");

                prepareManager();

                Set<Dependency> error = new LinkedHashSet<>();
                for (LockLoginDependencies lockloginDependency : LockLoginDependencies.values()) {
                    Dependency dependency = lockloginDependency.getAsDependency(obj -> {
                        File target = obj.getLocation();

                        try {
                            JarInjector injector = new JarInjector(target);
                            if (!injector.inject(this))
                                error.add(obj);
                        } catch (Throwable ex) {
                            error.add(obj);
                        }
                    });

                    if (dependency != null)
                        dependency.inject();
                }

                if (!error.isEmpty()) {
                    for (Dependency dependency : error) {
                        try {
                            File target = dependency.getLocation();

                            JarInjector injector = new JarInjector(target);
                            injector.download(dependency.getDownloadURL());

                            injector.inject(this);
                        } catch (Throwable ex) {
                            sendInjectionError(dependency.getName());
                            getServer().getPluginManager().disablePlugin(this);
                            break;
                        }
                    }

                    error.clear();
                }

                LockLogin.logger.setMaxSize(FileInfo.logFileSize(lockloginFile));
                LockLogin.logger.scheduleLog(Level.OK, "LockLogin initialized and all its dependencies has been loaded");

                File[] moduleFiles = LockLogin.getLoader().getDataFolder().listFiles();
                if (moduleFiles != null) {
                    for (File module : moduleFiles) {
                        if (!JavaModuleLoader.isLoaded(module.getName()))
                            LockLogin.getLoader().loadModule(module.getName());
                    }
                }

                PluginStatusChangeEvent event = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.LOAD, null);
                JavaModuleManager.callEvent(event);

                AllowedCommand.scan();

                Manager.initialize();
            });
        } else {
            sendInjectionError("KarmaAPI");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (injected) {
            PluginStatusChangeEvent event = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UNLOAD, null);
            JavaModuleManager.callEvent(event);

            File[] moduleFiles = LockLogin.getLoader().getDataFolder().listFiles();
            if (moduleFiles != null) {
                for (File module : moduleFiles) {
                    LockLogin.getLoader().unloadModule(module.getName());
                }
            }

            Manager.terminate();
        }
    }

    /**
     * Send the injection error message to
     * the console
     *
     * @param name the jar name
     */
    private void sendInjectionError(final String name) {
        Messages messages = new Messages();

        getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', messages.getProperty("plugin_injection_error", "&5&oLockLogin failed to inject dependency ( {0} )").replace("{0}", name)));
    }

    /**
     * Inject the plugin required API
     */
    private boolean injectAPI() {
        String version = FileInfo.getKarmaVersion(lockloginFile);
        File dest_file = new File(getDataFolder() + File.separator + "plugin" + File.separator + "api" + File.separator + "KarmaAPI" + File.separator + "custom", "KarmaAPI-Bundle.jar");

        if (dest_file.exists()) {
            System.out.println("Found custom KarmaAPI-Bundle.jar instance, trying to use it");

            JarManager manager = new JarManager(dest_file);
            return manager.inject(this.getClass());
        } else {
            dest_file = new File(getDataFolder() + File.separator + "plugin" + File.separator + "api" + File.separator + "KarmaAPI" + File.separator + version, "KarmaAPI-Bundle.jar");
            System.out.println("Preparing plugin for injection");

            JarManager manager = new JarManager(dest_file);
            Throwable result = manager.download("https://raw.githubusercontent.com/KarmaConfigs/project_c/main/src/libs/KarmaAPI/" + FileInfo.getKarmaVersion(lockloginFile) + "/KarmaAPI-Bundle.jar");

            if (result == null) {
                return manager.inject(this.getClass());
            } else {
                result.printStackTrace();
            }
        }

        return false;
    }

    /**
     * Prepare LockLoginManager module
     */
    private void prepareManager() {
        File dest_file = new File(getDataFolder() + File.separator + "plugin" + File.separator + "modules", "LockLoginManager.jar");

        Console.send(this, "Checking LockLogin manager module, please wait...", Level.INFO);

        JarManager manager = new JarManager(dest_file);
        Throwable result = manager.download("https://karmaconfigs.github.io/updates/LockLogin/modules/manager/LockLoginManager.jar");

        if (result != null) {
            Console.send(this, "LockLogin manager module is not downloaded/updated and couldn't be downloaded, apply updates and helpme command won't be available!", Level.GRAVE);
        }
    }
}
