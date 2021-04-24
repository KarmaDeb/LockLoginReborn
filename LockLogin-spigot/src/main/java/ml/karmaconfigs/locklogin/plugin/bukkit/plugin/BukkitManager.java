package ml.karmaconfigs.locklogin.plugin.bukkit.plugin;

import ml.karmaconfigs.api.bukkit.Console;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.LockLoginListener;
import ml.karmaconfigs.locklogin.api.event.plugin.PluginStatusChangeEvent;
import ml.karmaconfigs.locklogin.plugin.bukkit.permission.PluginPermission;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.Config;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.data.RestartCache;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.locklogin.plugin.common.utils.FileInfo;
import ml.karmaconfigs.locklogin.plugin.common.utils.enums.UpdateChannel;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;

import static ml.karmaconfigs.locklogin.plugin.bukkit.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bukkit.permission.PluginPermission.*;

public class BukkitManager {

    private static CommandSender update_issuer = null;

    /**
     * Unload LockLogin plugin
     */
    @SuppressWarnings("all")
    private static void unload() {
        String name = plugin.getName();

        PluginManager pluginManager = Bukkit.getPluginManager();

        SimpleCommandMap commandMap = null;

        List<Plugin> plugins = null;

        Map<String, Plugin> names = null;
        Map<String, Command> commands = null;
        Map<Event, SortedSet<RegisteredListener>> listeners = null;

        boolean reloadlisteners = true;
        pluginManager.disablePlugin(plugin);

        try {
            Field pluginsField = Bukkit.getPluginManager().getClass().getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            plugins = (List<Plugin>) pluginsField.get(pluginManager);

            Field lookupNamesField = Bukkit.getPluginManager().getClass().getDeclaredField("lookupNames");
            lookupNamesField.setAccessible(true);
            names = (Map<String, Plugin>) lookupNamesField.get(pluginManager);

            try {
                Field listenersField = Bukkit.getPluginManager().getClass().getDeclaredField("listeners");
                listenersField.setAccessible(true);
                listeners = (Map<Event, SortedSet<RegisteredListener>>) listenersField.get(pluginManager);
            } catch (Exception e) {
                reloadlisteners = false;
            }

            Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (SimpleCommandMap) commandMapField.get(pluginManager);

            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            commands = (Map<String, Command>) knownCommandsField.get(commandMap);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        pluginManager.disablePlugin(plugin);

        if (plugins != null && plugins.contains(plugin))
            plugins.remove(plugin);

        if (names != null && names.containsKey(name))
            names.remove(name);

        if (listeners != null && reloadlisteners) {
            for (SortedSet<RegisteredListener> set : listeners.values()) {
                set.removeIf(value -> value.getPlugin() == plugin);
            }
        }

        if (commandMap != null) {
            for (Iterator<Map.Entry<String, Command>> it = commands.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Command> entry = it.next();
                if (entry.getValue() instanceof PluginCommand) {
                    PluginCommand c = (PluginCommand) entry.getValue();
                    if (c.getPlugin() == plugin) {
                        c.unregister(commandMap);
                        it.remove();
                    }
                }
            }
        }

        // Attempt to close the classloader to unlock any handles on the plugin's jar file.
        ClassLoader cl = plugin.getClass().getClassLoader();

        if (cl instanceof URLClassLoader) {
            try {
                Field pluginField = cl.getClass().getDeclaredField("plugin");
                pluginField.setAccessible(true);
                pluginField.set(cl, null);

                Field pluginInitField = cl.getClass().getDeclaredField("pluginInit");
                pluginInitField.setAccessible(true);
                pluginInitField.set(cl, null);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }

            try {
                ((URLClassLoader) cl).close();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        //Remove all plugin static methods
        System.gc();
    }

    /**
     * Load LockLogin plugin
     *
     * @param file the plugin file
     */
    private static void load(final File file) {
        try {
            Plugin plugin = Bukkit.getPluginManager().loadPlugin(file);
            if (plugin != null)
                Bukkit.getPluginManager().enablePlugin(plugin);
        } catch (Throwable e) {
            logger.scheduleLog(Level.GRAVE, e);
            logger.scheduleLog(Level.INFO, "Error while loading LockLogin");
        }
    }

    /**
     * Auto-detect the update LockLogin instance
     *
     * @return the update LockLogin instance
     */
    @Nullable
    private static FileData getUpdateJar() {
        File update_folder = new File(plugin.getDataFolder(), "updater");
        if (update_folder.exists()) {
            File[] files = update_folder.listFiles();
            if (files != null) {
                File mostRecent = null;
                Instant lastInstant = null;
                String last_update_id = "";
                for (File file : files) {
                    if (!file.isDirectory()) {
                        String ext = FileUtilities.getExtension(file);
                        //We want this to only take jar files
                        if (ext.equals("jar")) {
                            String name = file.getName();
                            //LockLogin updater will always download LockLogin_<download time instant>_<version id>.jar
                            if (name.contains("_")) {
                                String[] nameData = name.split("_");

                                //If the data of jar name is not an instant, ignore it
                                try {
                                    Instant instant = Instant.parse(nameData[1]);

                                    if (lastInstant != null) {
                                        if (instant.isAfter(lastInstant)) {
                                            mostRecent = file;
                                            last_update_id = nameData[2];
                                        }
                                    } else {
                                        lastInstant = instant;
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                }

                return new FileData(mostRecent, lastInstant, last_update_id);
            }
        }

        return null;
    }

    /**
     * Completely update the plugin
     *
     * @param issuer the issuer that called the update
     */
    public static void update(final CommandSender issuer) {
        Config config = new Config();
        Message messages = new Message();

        if (update_issuer instanceof Player) {
            Player player = (Player) update_issuer;
            if (!player.isOnline())
                update_issuer = issuer;
        }

        if (update_issuer == null && update_issuer != issuer) {
            update_issuer = issuer;

            //This process is async as update jar fetching could take a long time
            //depending on the host or update jars amount
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                FileData last_jar = getUpdateJar();
                File curr_jar = lockloginFile;

                if (last_jar != null) {
                    UpdateChannel current_channel = config.getUpdaterOptions().getChannel();
                    UpdateChannel update_channel = FileInfo.getChannel(last_jar.getFile());
                    String last_version = FileInfo.getJarVersion(last_jar.getFile());

                    int latest = parseInteger(last_version);
                    int current = parseInteger(version);

                    boolean versionCriteria = latest >= current;
                    if (!versionCriteria) {
                        if (FileInfo.unsafeUpdates(last_jar.getFile())) {
                            if (update_issuer instanceof Player || update_issuer instanceof RemoteConsoleCommandSender) {
                                if (update_issuer.hasPermission(applyUnsafeUpdates())) {
                                    versionCriteria = true;
                                } else {
                                    update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + messages.permissionError(applyUnsafeUpdates())));
                                }
                            } else {
                                versionCriteria = true;
                            }
                        }
                    }

                    //Make sure the latest jar version id is not the current one
                    if (!last_jar.getVersion().equals(versionID) && versionCriteria) {
                        switch (current_channel) {
                            case RELEASE:
                                if (update_channel.equals(UpdateChannel.RELEASE)) {
                                    update(last_jar.getFile(), curr_jar);
                                } else {
                                    update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_not_available", "&cUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )")));
                                }
                                break;
                            case RC:
                                switch (update_channel) {
                                    case RELEASE:
                                    case RC:
                                        update(last_jar.getFile(), curr_jar);
                                        break;
                                    default:
                                        update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_not_available", "&cUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )")));
                                        break;
                                }
                            case SNAPSHOT:
                                update(last_jar.getFile(), curr_jar);
                                break;
                            default:
                                update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_not_available", "&cUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )")));
                        }
                    } else {
                        update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_not_available", "&cUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )")));
                    }
                } else {
                    update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_not_available", "&cUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )")));
                }
            });
        } else {
            if (update_issuer != null && update_issuer != issuer)
                issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_already_updating", "&cUpdate process is already being processed by {0}"), update_issuer.getName())));
        }
    }

    /**
     * Reload the plugin files and other
     *
     * @param player the player that called the action
     */
    public static void reload(final Player player) {
        PluginStatusChangeEvent reload_start = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.RELOAD_START, null);
        LockLoginListener.callEvent(reload_start);

        Message message = new Message();
        if (player != null) {
            User user = new User(player);

            if (player.hasPermission(PluginPermission.reload_config()) || player.hasPermission(PluginPermission.reload_messages())) {
                if (player.hasPermission(PluginPermission.reload_config())) {
                    if (Config.manager.reload()) {
                        Config.manager.checkValues();

                        user.send(message.prefix() + properties.getProperty("reload_config", "&aReloaded config file"));
                    }
                }
                if (player.hasPermission(PluginPermission.reload_messages())) {
                    if (Message.manager.reload()) {
                        user.send(message.prefix() + properties.getProperty("reload_messages", "&aReloaded messages file"));
                    }
                }

                if (player.hasPermission(PluginPermission.reload())) {
                    user.send(message.prefix() + properties.getProperty("restart_systems", "&7Restarting version checker and plugin alert systems"));

                    Manager.restartVersionChecker();
                    Manager.restartAlertSystem();
                }
            } else {
                user.send(message.prefix() + message.permissionError(PluginPermission.reload()));
            }
        } else {
            if (Config.manager.reload()) {
                Config.manager.checkValues();

                Console.send(message.prefix() + properties.getProperty("reload_config", "&aReloaded config file"));
            }
            if (Message.manager.reload()) {
                Console.send(message.prefix() + properties.getProperty("reload_messages", "&aReloaded messages file"));
            }

            Console.send(message.prefix() + properties.getProperty("restart_systems", "&7Restarting version checker and plugin alert systems"));

            Manager.restartVersionChecker();
            Manager.restartAlertSystem();
        }

        PluginStatusChangeEvent reload_finish = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.RELOAD_END, null);
        LockLoginListener.callEvent(reload_finish);
    }

    /**
     * Update the plugin instance
     *
     * @param last_jar the latest jar file
     * @param curr_jar the current jar file
     */
    private static void update(final File last_jar, final File curr_jar) {
        Message messages = new Message();

        RestartCache cache = new RestartCache();
        cache.storeSessions();
        cache.storeBungeeKey();

        PluginStatusChangeEvent update_start = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UPDATE_START, null);
        LockLoginListener.callEvent(update_start);

        unload();
        update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_unloaded", "&aUnloaded current LockLogin instance and prepared new instance...")));

        Timer move_timer = new Timer();
        move_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Files.move(last_jar.toPath(), curr_jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Throwable ignored) {}

                update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_moved", "&aMoved new plugin instance replacing the current one...")));

                Timer load_timer = new Timer();
                load_timer.schedule(new TimerTask() {
                    int back = 5;
                    @Override
                    public void run() {
                        if (back != 0) {
                            update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_loading", "&aLoading new plugin instance in {0} seconds"), back)));
                        } else {
                            load_timer.cancel();
                            load(curr_jar);

                            PluginStatusChangeEvent update_end = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UPDATE_END, null);
                            LockLoginListener.callEvent(update_end);
                        }
                        back--;
                    }
                }, 0, 1000);
            }
        }, 3000);
    }

    /**
     * Parse only the integer values of the
     * string
     *
     * @param input the string input
     * @return the numbers of the string
     */
    private static int parseInteger(final String input) {
        StringBuilder builder = new StringBuilder();
        //Append 0 value in case the string does not have
        //any number
        builder.append("0");

        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (Character.isDigit(character))
                builder.append(character);
        }

        return Integer.parseInt(builder.toString());
    }
}

@SuppressWarnings("unused")
class FileData {

    private final File file;
    private final Instant update_time;
    private final String version_id;

    /**
     * Initialize the file data
     *
     * @param update_jar the update LockLogin file
     * @param date the download date of the file
     * @param version the jar version id
     */
    public FileData(final File update_jar, final Instant date, final String version) {
        file = update_jar;
        update_time = date;
        version_id = version;
    }

    /**
     * Get the update jar file
     *
     * @return the update jar file
     */
    public final File getFile() {
        return file;
    }

    /**
     * Get the update jar download time
     *
     * @return the update jar download time
     */
    public final Instant getDate() {
        return update_time;
    }

    /**
     * Get the update jar version id
     *
     * @return the update jar version id
     */
    public final String getVersion() {
        return version_id;
    }
}
