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

import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.api.common.utils.StringUtils;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginStatusChangeEvent;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleManager;
import eu.locklogin.api.util.enums.UpdateChannel;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.util.files.Message;
import eu.locklogin.plugin.bukkit.util.files.data.RestartCache;
import eu.locklogin.api.common.utils.FileInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
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

import static eu.locklogin.plugin.bukkit.plugin.PluginPermission.applyUnsafeUpdates;
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
    private static FileData getUpdateJar() {
        File update_folder = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "plugin", "updater");
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
                                    Instant instant = Instant.parse(nameData[1].replace(";", ":"));
                                    String versionId = nameData[2];

                                    if (lastInstant != null) {
                                        if (instant.isAfter(lastInstant)) {
                                            mostRecent = file;
                                            last_update_id = versionId;
                                        }
                                    } else {
                                        lastInstant = instant;
                                        mostRecent = file;
                                        last_update_id = versionId;
                                    }
                                } catch (Throwable ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }

                if (mostRecent != null && lastInstant != null && !last_update_id.replaceAll("\\s", "").isEmpty())
                    return new FileData(mostRecent, lastInstant, last_update_id);
                else
                    return null;
            }
        }

        return null;
    }

    /**
     * Completely update the plugin
     *
     * @param issuer the issuer that called the update
     * @param unsafe if the issuer is able to perform unsafe updates
     */
    public static void update(final CommandSender issuer, final boolean unsafe) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        Message messages = new Message();

        if (update_issuer == null) {
            update_issuer = issuer;

            FileData last_jar = getUpdateJar();
            File curr_jar = new File(FileUtilities.getPluginsFolder(), lockloginFile.getName());

            if (last_jar != null) {
                UpdateChannel current_channel = config.getUpdaterOptions().getChannel();
                UpdateChannel update_channel = FileInfo.getChannel(last_jar.getFile());
                String last_version = FileInfo.getJarVersion(last_jar.getFile());

                int latest = parseInteger(last_version);
                int current = parseInteger(version);

                boolean versionCriteria = latest == current || latest > current;
                if (!versionCriteria) {
                    if (FileInfo.unsafeUpdates(last_jar.getFile())) {
                        if (update_issuer instanceof Player || update_issuer instanceof RemoteConsoleCommandSender) {
                            if (unsafe) {
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
                                update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "update channel is over the current one", "no second reason")));
                            }
                            break;
                        case RC:
                            switch (update_channel) {
                                case RELEASE:
                                case RC:
                                    update(last_jar.getFile(), curr_jar);
                                    break;
                                default:
                                    update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "update channel is over the current one", "no second reason")));
                                    break;
                            }
                        case SNAPSHOT:
                            update(last_jar.getFile(), curr_jar);
                            break;
                        default:
                            update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "unknown update channel", "no second reason")));
                    }
                } else {
                    update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"),
                            !last_jar.getVersion().equals(versionID), versionCriteria)));
                }
            } else {
                update_issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "jar file is null", "no second reason")));
            }

            update_issuer = null;
        } else {
            if (update_issuer != issuer)
                issuer.sendMessage(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_already_updating", "&5&oUpdate process is already being processed by {0}"), update_issuer.getName())));
        }
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

    /**
     * Update the LockLogin instance
     *
     * @param update_jar  the update jar file
     * @param current_jar the current jar file
     */
    private static void update(final File update_jar, final File current_jar) {
        CommandSender issuer = update_issuer;
        Message messages = new Message();

        RestartCache cache = new RestartCache();
        cache.storeUserData();
        cache.storeBungeeKey();

        PluginStatusChangeEvent update_start = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UPDATE_START, null);
        JavaModuleManager.callEvent(update_start);

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

                            PluginStatusChangeEvent update_end = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UPDATE_END, null);
                            JavaModuleManager.callEvent(update_end);
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
