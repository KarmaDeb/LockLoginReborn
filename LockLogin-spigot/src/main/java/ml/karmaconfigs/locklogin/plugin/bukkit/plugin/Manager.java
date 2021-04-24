package ml.karmaconfigs.locklogin.plugin.bukkit.plugin;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.bukkit.karmayaml.FileCopy;
import ml.karmaconfigs.api.bukkit.timer.AdvancedPluginTimer;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.plugin.bukkit.Main;
import ml.karmaconfigs.locklogin.plugin.bukkit.command.*;
import ml.karmaconfigs.locklogin.plugin.bukkit.listener.JoinListener;
import ml.karmaconfigs.locklogin.plugin.bukkit.listener.QuitListener;
import ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee.BungeeReceiver;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.client.PlayerFile;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.Config;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.RestartCache;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.Session;
import ml.karmaconfigs.locklogin.plugin.common.security.client.Proxy;
import ml.karmaconfigs.locklogin.plugin.common.utils.ASCIIArtGenerator;
import ml.karmaconfigs.locklogin.plugin.common.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.common.web.AlertSystem;
import ml.karmaconfigs.locklogin.plugin.common.web.VersionChecker;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;

public final class Manager {

    private static int changelog_requests = 0;
    private static int updater_id = 0;
    private static int alert_id = 0;

    public static void initialize() {
        int size = 10;
        String character = "*";
        try {
            size = Integer.parseInt(properties.getProperty("ascii_art_size", "10"));
            character = properties.getProperty("ascii_art_character", "*").substring(0, 1);
        } catch (Throwable ignored) {}

        System.out.println();
        artGen.print(ml.karmaconfigs.api.common.Console.Colors.YELLOW_BRIGHT, "LockLogin", size, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, character);
        Console.send("&eversion:&6 {0}", versionID);

        Proxy.scan();

        PlayerFile.migrateV1();
        PlayerFile.migrateV2();

        setupFiles();
        registerCommands();
        registerListeners();

        Console.send(" ");
        Console.send("&e-----------------------");

        if (!CurrentPlatform.isValidAccountManager()) {
            CurrentPlatform.setAccountsManager(PlayerFile.class);
            Console.send(plugin, "Loaded native player account manager", Level.INFO);
        } else {
            Console.send(plugin, "Loaded custom player account manager", Level.INFO);
        }
        if (!CurrentPlatform.isValidSessionManager()) {
            CurrentPlatform.setSessionManager(Session.class);
            Console.send(plugin, "Loaded native player session manager", Level.INFO);
        } else {
            Console.send(plugin, "Loaded custom player session manager", Level.INFO);
        }

        Config config = new Config();
        if (config.isBungeeCord()) {
            Messenger messenger = plugin.getServer().getMessenger();
            BungeeReceiver receiver = new BungeeReceiver();

            PluginMessageListenerRegistration registration_account = messenger.registerIncomingPluginChannel(plugin, "ll_account", receiver);
            PluginMessageListenerRegistration registration_plugin = messenger.registerIncomingPluginChannel(plugin, "ll_plugin", receiver);

            if (registration_account.isValid() && registration_plugin.isValid()) {
                Console.send(plugin, "Registered plugin message listeners", Level.OK);
            } else {
                Console.send(plugin, "Something went wrong while trying to register message listeners, things may not work properly", Level.GRAVE);
            }
        }
    }

    public static void terminate() {
        int size = 10;
        String character = "*";
        try {
            size = Integer.parseInt(properties.getProperty("ascii_art_size", "10"));
            character = properties.getProperty("ascii_art_character", "*").substring(0, 1);
        } catch (Throwable ignored) {}

        System.out.println();
        artGen.print(ml.karmaconfigs.api.common.Console.Colors.RED_BRIGHT, "LockLogin", size, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, character);
        Console.send("&eversion:&6 {0}", versionID);
        Console.send(" ");
        Console.send("&e-----------------------");
    }

    /**
     * Register plugin commands
     */
    protected static void registerCommands() {
        Set<String> unregistered = new LinkedHashSet<>();

        PluginCommand locklogin = plugin.getCommand("locklogin");
        PluginCommand setspawn = plugin.getCommand("setloginspawn");
        PluginCommand register = plugin.getCommand("register");
        PluginCommand login = plugin.getCommand("login");
        PluginCommand gAuth = plugin.getCommand("2fa");
        PluginCommand playerinfo = plugin.getCommand("playerinfo");
        PluginCommand delaccount = plugin.getCommand("delaccount");
        PluginCommand unlock = plugin.getCommand("unlock");
        if (locklogin != null) {
            LockLoginCommand lockloginExecutor = new LockLoginCommand();
            locklogin.setExecutor(lockloginExecutor);
        } else {
            unregistered.add("locklogin");
        }
        if (setspawn != null) {
            SetSpawnCommand setspawnExecutor = new SetSpawnCommand();
            setspawn.setExecutor(setspawnExecutor);
        } else {
            unregistered.add("setloginspawn");
        }
        if (register != null) {
            RegisterCommand registerExecutor = new RegisterCommand();
            register.setExecutor(registerExecutor);
        } else {
            unregistered.add("register");
        }
        if (login != null) {
            LoginCommand loginExecutor = new LoginCommand();
            login.setExecutor(loginExecutor);
        } else {
            unregistered.add("login");
        }
        if (gAuth != null) {
            GoogleAuthCommand gAuthExecutor = new GoogleAuthCommand();
            gAuth.setExecutor(gAuthExecutor);
        } else {
            unregistered.add("2fa");
        }
        if (playerinfo != null) {
            PlayerInfoCommand playerinfoExecutor = new PlayerInfoCommand();
            playerinfo.setExecutor(playerinfoExecutor);
        } else {
            unregistered.add("playerinfo");
        }
        if (delaccount != null) {
            DelAccountCommand delaccountExecutor = new DelAccountCommand();
            delaccount.setExecutor(delaccountExecutor);
        } else {
            unregistered.add("delaccount");
        }
        if (unlock != null) {
            UnLockCommand unlockExecutor = new UnLockCommand();
            unlock.setExecutor(unlockExecutor);
        } else {
            unregistered.add("unlock");
        }

        if (!unregistered.isEmpty()) {
            Console.send(plugin, properties.getProperty("command_register_problem", "Failed to register command(s): {0}"), Level.GRAVE, setToString(unregistered));
            Console.send(plugin, properties.getProperty("plugin_error_disabling", "Disabling plugin due an internal error"), Level.INFO);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    /**
     * Setup the plugin files
     */
    protected static void setupFiles() {
        Set<String> failed = new LinkedHashSet<>();

        File cfg = new File(plugin.getDataFolder(), "config.yml");

        FileCopy config_copy = new FileCopy(plugin, "config.yml");
        try {
            config_copy.copy(cfg);
        } catch (Throwable ex) {
            failed.add("config.yml");
        }

        Config config = new Config();

        String country = config.getLang().country(config.getLangName());
        File msg_file = new File(plugin.getDataFolder() + File.separator + "lang_v2", "messages_" + country + ".yml");

        InputStream internal = Main.class.getResourceAsStream("/lang/messages_" + country + ".yml");
        //Check if the file exists inside the plugin as an official language
        if (internal != null) {
            if (!msg_file.exists()) {
                FileCopy copy = new FileCopy(plugin, "messages_" + country + ".yml");

                try {
                    copy.copy(msg_file);
                } catch (Throwable ex) {
                    failed.add(msg_file.getName());
                }
            }
        } else {
            if (!msg_file.exists()) {
                failed.add(msg_file.getName());
                Console.send(plugin, "Could not find community message pack named {0} in lang_v2 folder, using messages english as default", Level.GRAVE, msg_file.getName());

                msg_file = new File(plugin.getDataFolder() + File.separator + "lang_v2", "messages_en.yml");

                if (!msg_file.exists()) {
                    FileCopy copy = new FileCopy(plugin, "lang/messages_en.yml");

                    try {
                        copy.copy(msg_file);
                    } catch (Throwable ex) {
                        failed.add(msg_file.getName());
                    }
                }
            } else {
                Console.send(plugin, "Detected community language pack, please make sure this pack is updated to avoid translation errors", Level.WARNING);
            }
        }

        if (!failed.isEmpty()) {
            Console.send(plugin, properties.getProperty("file_register_problem", "Failed to setup/check file(s): {0}. The plugin will use defaults, you can try to create files later by running /locklogin reload"), Level.WARNING, setToString(failed));
        }
    }

    /**
     * Register the plugin listeners
     */
    protected static void registerListeners() {
        Listener onJoin = new JoinListener();
        Listener onQuit = new QuitListener();

        plugin.getServer().getPluginManager().registerEvents(onJoin, plugin);
        plugin.getServer().getPluginManager().registerEvents(onQuit, plugin);
    }

    /**
     * Load the plugin cache if exists
     */
    protected static void loadCache() {
        RestartCache cache = new RestartCache();
        cache.loadSessions();

        cache.remove();
    }

    /**
     * Perform a version check
     */
    protected static void performVersionCheck() {
        Config config = new Config();

        VersionChecker checker = new VersionChecker(versionID);
        checker.checkVersion(config.getUpdaterOptions().getChannel());

        if (checker.isOutdated()) {
            if (changelog_requests <= 0) {
                changelog_requests = 3;

                Console.send(checker.getChangelog());
            } else {
                changelog_requests--;
            }
        }
    }

    /**
     * Schedule the version check process
     */
    protected static void scheduleVersionCheck() {
        Config config = new Config();

        AdvancedPluginTimer timer = new AdvancedPluginTimer(plugin, config.getUpdaterOptions().getInterval(), true).setAsync(true).addActionOnEnd(Manager::performVersionCheck);
        if (config.getUpdaterOptions().isEnabled())
            timer.start();

        updater_id = timer.getTimerId();

    }

    /**
     * Schedule the alert system
     */
    protected static void scheduleAlertSystem() {
        AdvancedPluginTimer timer = new AdvancedPluginTimer(plugin, 30, true).setAsync(true).addActionOnEnd(() -> {
            AlertSystem system = new AlertSystem();
            system.checkAlerts();

            if (system.available())
                Console.send(system.getMessage());
        });
        timer.start();

        alert_id = timer.getTimerId();

    }

    /**
     * Restart the version checker
     */
    public static void restartVersionChecker() {
        try {
            AdvancedPluginTimer timer = AdvancedPluginTimer.getManager.getTimer(updater_id);
            timer.setCancelled();
        } catch (Throwable ignored) {}

        scheduleVersionCheck();
    }

    /**
     * Restart the alert system timer
     */
    public static void restartAlertSystem() {
        try {
            AdvancedPluginTimer timer = AdvancedPluginTimer.getManager.getTimer(alert_id);
            timer.setCancelled();
        } catch (Throwable ignored) {}

        scheduleAlertSystem();
    }

    /**
     * Convert a set of strings into a single string
     *
     * @param set the set to convert
     * @return the converted set
     */
    private static String setToString(final Set<String> set) {
        StringBuilder builder = new StringBuilder();
        for (String str : set) {
            builder.append(str.replace(",", "comma")).append(", ");
        }

        return StringUtils.replaceLast(builder.toString(), ", ", "");
    }
}
