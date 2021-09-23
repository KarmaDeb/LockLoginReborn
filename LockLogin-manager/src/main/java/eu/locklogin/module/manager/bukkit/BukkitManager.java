package eu.locklogin.module.manager.bukkit;

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

import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginStatusChangeEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.enums.UpdateChannel;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.util.files.data.RestartCache;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;
import ml.karmaconfigs.api.common.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

public class BukkitManager {

    private static CommandSender update_issuer = null;

    /**
     * Unload LockLogin plugin
     */
    @SuppressWarnings("all")
    public static void unload() {
        Plugin locklogin = Bukkit.getPluginManager().getPlugin("LockLogin");
        String name = locklogin.getName();

        PluginManager pluginManager = Bukkit.getPluginManager();

        SimpleCommandMap commandMap = null;

        List<Plugin> plugins = null;

        Map<String, Plugin> names = null;
        Map<String, Command> commands = null;
        Map<Event, SortedSet<RegisteredListener>> listeners = null;

        boolean reloadlisteners = true;

        if (pluginManager != null) {
            pluginManager.disablePlugin(locklogin);
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
        }

        pluginManager.disablePlugin(locklogin);

        if (plugins != null && plugins.contains(locklogin))
            plugins.remove(locklogin);

        if (names != null && names.containsKey(name))
            names.remove(name);

        if (listeners != null && reloadlisteners) for (SortedSet<RegisteredListener> set : listeners.values())
            for (Iterator<RegisteredListener> it = set.iterator(); it.hasNext(); ) {
                RegisteredListener value = it.next();
                if (value.getPlugin() == locklogin) it.remove();
            }

        if (commandMap != null)
            for (Iterator<Map.Entry<String, Command>> it = commands.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Command> entry = it.next();
                if (entry.getValue() instanceof PluginCommand) {
                    PluginCommand c = (PluginCommand) entry.getValue();
                    if (c.getPlugin() == locklogin) {
                        c.unregister(commandMap);
                        it.remove();
                    }
                }
            }

        // Attempt to close the classloader to unlock any handles on the plugin's jar file.
        ClassLoader cl = Main.class.getClassLoader();

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

        pluginManager.disablePlugin(locklogin);

        // Will not work on processes started with the -XX:+DisableExplicitGC flag, but lets try it anyway.
        // This tries to get around the issue where Windows refuses to unlock jar files that were previously loaded into the JVM.
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
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Auto-detect the update LockLogin instance
     *
     * @return the update LockLogin instance
     */
    private static File getUpdateJar() {
        File update_folder = new File(plugin.getDataFolder() + File.separator + "plugin", "updater");
        if (update_folder.exists()) {
            Set<File> major_files = new LinkedHashSet<>();
            File[] update_files = update_folder.listFiles();
            if (update_files != null) {
                for (File file : update_files) {
                    String extension = FileUtilities.getExtension(file);
                    if (extension.equals("jar")) {
                        String version = FileInfo.getJarVersion(file);
                        String current = versionID.resolve(versionID.getVersionID());

                        if (StringUtils.compareTo(version, current) > 0) {
                            major_files.add(file);
                        } else {
                            try {
                                Files.deleteIfExists(file.toPath());
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }

            if (major_files.size() > 0) {
                if (major_files.size() == 1) {
                    return major_files.toArray(new File[0])[0];
                } else {
                    File latest_major = null;
                    for (File major : major_files) {
                        if (latest_major != null) {
                            String latest_major_version = FileInfo.getJarVersion(latest_major);
                            String current_major_version = FileInfo.getJarVersion(major);

                            if (StringUtils.compareTo(latest_major_version, current_major_version) > 0) {
                                try {
                                    Files.deleteIfExists(latest_major.toPath());
                                } catch (Throwable ignored) {}
                                latest_major = major;
                            }
                        } else {
                            latest_major = major;
                        }
                    }

                    return latest_major;
                }
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
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PluginMessages messages = CurrentPlatform.getMessages();

        if (update_issuer == null) {
            update_issuer = issuer;

            File last_jar = getUpdateJar();
            File curr_jar = new File(FileUtilities.getProjectFolder(), lockloginFile.getName());

            if (last_jar != null) {
                UpdateChannel current_channel = config.getUpdaterOptions().getChannel();
                UpdateChannel update_channel = FileInfo.getChannel(last_jar);

                //Make sure the latest jar version id is not the current one
                switch (current_channel) {
                    case RELEASE:
                        if (update_channel.equals(UpdateChannel.RELEASE)) {
                            update(last_jar, curr_jar);
                        } else {
                            update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "update channel is over the current one", "no second reason")));
                        }
                        break;
                    case RC:
                        switch (update_channel) {
                            case RELEASE:
                            case RC:
                                update(last_jar, curr_jar);
                                break;
                            default:
                                update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "update channel is over the current one", "no second reason")));
                                break;
                        }
                    case SNAPSHOT:
                        update(last_jar, curr_jar);
                        break;
                    default:
                        update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "unknown update channel", "no second reason")));
                        break;
                }
            } else {
                update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file is null or jar version is lower than current )"))));
            }

            update_issuer = null;
        } else {
            if (update_issuer != issuer)
                issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_already_updating", "&5&oUpdate process is already being processed by {0}"), update_issuer.getName())));
        }
    }

    /**
     * Update the LockLogin instance
     *
     * @param update_jar  the update jar file
     * @param current_jar the current jar file
     */
    private static void update(final File update_jar, final File current_jar) {
        CommandSender issuer = update_issuer;
        PluginMessages messages = CurrentPlatform.getMessages();

        RestartCache cache = new RestartCache();
        cache.storeUserData();
        cache.storeBungeeKey();

        eu.locklogin.api.module.plugin.api.event.util.Event update_start = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UPDATE_START, null);
        ModulePlugin.callEvent(update_start);

        Timer load_timer = new Timer();
        load_timer.schedule(new TimerTask() {
            int back = 8;

            @Override
            public void run() {
                if (back != 0 && back <= 5) {
                    issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_loading", "&dLoading new plugin instance in {0} seconds"), back)));
                } else {
                    if (back == 0) {
                        load_timer.cancel();
                        try {
                            Files.move(update_jar.toPath(), current_jar.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            issuer.sendMessage(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_moved", "&dMoved new plugin instance, replacing the current one...")));
                            load(current_jar);

                            eu.locklogin.api.module.plugin.api.event.util.Event update_end = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UPDATE_END, null);
                            ModulePlugin.callEvent(update_end);
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                            try {
                                Files.copy(update_jar.toPath(), current_jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            } catch (Throwable exc) {
                                exc.printStackTrace();
                            }
                        }
                    }
                }
                back--;
            }
        }, 0, 1000);

        unload();
        issuer.sendMessage(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_unloaded", "&dUnloaded current LockLogin instance and prepared new instance...")));
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
     * @param date       the download date of the file
     * @param version    the jar version id
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
