package ml.karmaconfigs.locklogin.plugin.velocity;

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

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import ml.karmaconfigs.api.common.JarInjector;
import ml.karmaconfigs.api.common.KarmaAPI;
import ml.karmaconfigs.api.common.KarmaPlugin;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.velocity.Console;
import ml.karmaconfigs.api.velocity.Util;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.modules.api.channel.ModuleMessageService;
import ml.karmaconfigs.locklogin.api.modules.api.event.plugin.PluginStatusChangeEvent;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.UserAuthenticateEvent;
import ml.karmaconfigs.locklogin.api.modules.util.client.MessageSender;
import ml.karmaconfigs.locklogin.api.modules.util.client.ModulePlayer;
import ml.karmaconfigs.locklogin.api.modules.util.dependencies.Dependency;
import ml.karmaconfigs.locklogin.api.modules.util.dependencies.LockLoginDependencies;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleLoader;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.api.utils.platform.Platform;
import ml.karmaconfigs.locklogin.plugin.common.JarManager;
import ml.karmaconfigs.locklogin.plugin.common.security.AllowedCommand;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import ml.karmaconfigs.locklogin.plugin.common.utils.FileInfo;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.Messages;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.Manager;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;
import net.kyori.adventure.text.Component;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Plugin(id = "locklogin", name = "LockLogin", version = "1.12.7", authors = {"KarmaDev"}, description =
        "LockLogin is an advanced login plugin, one of the most secure available, with tons of features. " +
                "It has a lot of customization options to not say " +
                "almost everything is customizable. Regular updates and one of the bests discord supports " +
                "( according to spigotmc reviews ). LockLogin is a plugin " +
                "always open to new feature requests, and bug reports. More than a plugin, a plugin you can contribute" +
                "indirectly; A community plugin for the plugin community.", url = "https://karmaconfigs.ml/")
@KarmaPlugin(plugin_name = "LockLogin", plugin_version = "1.12.7")
public class Main {

    private final static File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));

    static ProxyServer server;
    static PluginContainer plugin;
    static Metrics.Factory factory;

    private static boolean injected = false;
    private static Main instance;

    @Inject
    public Main(ProxyServer server, Logger logger, Metrics.Factory fact) {
        Main.server = server;
        factory = fact;
    }

    /**
     * Get a main instance
     *
     * @return this instance
     */
    public static Main get() {
        return instance;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onProxyInitialization(ProxyInitializeEvent e) {
        new Thread(() -> {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    instance = Main.this;
                    injected = injectAPI();

                    if (injected) {
                        Console.send("&aInjected plugin KarmaAPI version {0}, compiled at {1} for jdk {2}", KarmaAPI.getVersion(), KarmaAPI.getBuildDate(), KarmaAPI.getCompilerVersion());
                        Optional<PluginContainer> container = server.getPluginManager().getPlugin("locklogin");

                        if (container.isPresent()) {
                            plugin = container.get();

                            Util util = new Util(plugin);
                            util.initialize();

                            CurrentPlatform.setPlatform(Platform.VELOCITY);
                            CurrentPlatform.setMain(Main.class);
                            CurrentPlatform.setOnline(server.getConfiguration().isOnlineMode());

                            Consumer<MessageSender> onMessage = messageSender -> {
                                ModulePlayer modulePlayer = messageSender.getPlayer();
                                UUID id = modulePlayer.getUUID();

                                Optional<com.velocitypowered.api.proxy.Player> client = server.getPlayer(id);
                                client.ifPresent(value -> value.sendMessage(Component.text().content(StringUtils.toColor(messageSender.getMessage())).build()));
                            };
                            Consumer<ModulePlayer> onLogin = modulePlayer -> {
                                UUID id = modulePlayer.getUUID();

                                Optional<Player> tmp_player = server.getPlayer(id);
                                if (tmp_player.isPresent()) {
                                    Player player = tmp_player.get();
                                    User user = new User(player);
                                    ClientSession session = user.getSession();

                                    session.setCaptchaLogged(true);
                                    session.setLogged(true);
                                    session.setPinLogged(true);
                                    session.set2FALogged(true);

                                    DataSender.MessageData login = DataSender.getBuilder(DataType.SESSION, DataSender.CHANNEL_PLAYER, player).build();
                                    DataSender.MessageData pin = DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER, player).addTextData("close").build();
                                    DataSender.MessageData gauth = DataSender.getBuilder(DataType.GAUTH, DataSender.CHANNEL_PLAYER, player).build();

                                    DataSender.send(player, login);
                                    DataSender.send(player, pin);
                                    DataSender.send(player, gauth);

                                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.API, UserAuthenticateEvent.Result.SUCCESS, LockLogin.fromPlayer(player), "", null);
                                    JavaModuleManager.callEvent(event);

                                    user.checkServer();
                                }
                            };
                            Consumer<ModulePlayer> onClose = modulePlayer -> {
                                UUID id = modulePlayer.getUUID();

                                Optional<Player> tmp_player = server.getPlayer(id);
                                if (tmp_player.isPresent()) {
                                    Player player = tmp_player.get();
                                    User user = new User(player);
                                    user.performCommand("account close");
                                }
                            };
                            BiConsumer<String, byte[]> onDataSend = DataSender::sendModule;

                            try {
                                JarManager.changeField(ModulePlayer.class, "onChat", true, onMessage);
                                JarManager.changeField(ModulePlayer.class, "onLogin", true, onLogin);
                                JarManager.changeField(ModulePlayer.class, "onClose", true, onClose);
                                JarManager.changeField(ModuleMessageService.class, "onDataSent", true, onDataSend);
                            } catch (Throwable ignored) {}

                            Console.setOkPrefix(plugin, "&aOk &e>> &7");
                            Console.setInfoPrefix(plugin, "&7Info &e>> &7");
                            Console.setWarningPrefix(plugin, "&6Warning &e>> &7");
                            Console.setGravePrefix(plugin, "&4Grave &e>> &7");

                            prepareManager();

                            Set<Dependency> error = new LinkedHashSet<>();
                            for (LockLoginDependencies lockloginDependency : LockLoginDependencies.values()) {
                                Dependency dependency = lockloginDependency.getAsDependency(obj -> {
                                    File target = obj.getLocation();

                                    try {
                                        JarInjector injector = new JarInjector(target);
                                        if (!injector.inject(plugin))
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

                                        injector.inject(plugin);
                                    } catch (Throwable ex) {
                                        sendInjectionError(dependency.getName());
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
                        } else {
                            server.getConsoleCommandSource().sendMessage(Component.text().content(StringUtils.toColor("&cTried to load LockLogin but is not even loaded by velocity!")).build());
                        }
                    } else {
                        sendInjectionError("KarmaAPI");
                    }
                }
            }, TimeUnit.SECONDS.convert(5, TimeUnit.MILLISECONDS));
        }).start();
    }

    @Subscribe(order = PostOrder.LAST)
    public void onProxyInitialization(ProxyShutdownEvent e) {
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

        server.getConsoleCommandSource().sendMessage(Component.text().content(messages.getProperty("plugin_injection_error", "&5&oLockLogin failed to inject dependency ( {0} )").replace("{0}", name)));
    }

    /**
     * Inject the plugin required API
     */
    private boolean injectAPI() {
        String version = FileInfo.getKarmaVersion(lockloginFile);
        File dest_file = new File(lockloginFile.getParentFile() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "api" + File.separator + "KarmaAPI" + File.separator + "custom", "KarmaAPI-Bundle.jar");

        if (dest_file.exists()) {
            System.out.println("Found custom KarmaAPI-Bundle.jar instance, trying to use it");

            JarManager manager = new JarManager(dest_file);
            return manager.inject(this.getClass());
        } else {
            dest_file = new File(lockloginFile.getParentFile() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "api" + File.separator + "KarmaAPI" + File.separator + version, "KarmaAPI-Bundle.jar");
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
        File dest_file = new File(lockloginFile.getParentFile() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", "LockLoginManager.jar");

        Console.send(plugin, "Checking LockLogin manager module, please wait...", Level.INFO);

        JarManager manager = new JarManager(dest_file);
        Throwable result = manager.download("https://karmaconfigs.github.io/updates/LockLogin/modules/manager/LockLoginManager.jar");

        if (result != null) {
            Console.send(plugin, "LockLogin manager module is not downloaded/updated and couldn't be downloaded, apply updates and helpme command won't be available!", Level.GRAVE);
        }
    }
}
