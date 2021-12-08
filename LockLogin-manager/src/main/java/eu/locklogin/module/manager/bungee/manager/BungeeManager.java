package eu.locklogin.module.manager.bungee.manager;

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
import eu.locklogin.plugin.bungee.Main;
import eu.locklogin.plugin.bungee.util.files.data.RestartCache;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.common.utils.string.VersionComparator;
import ml.karmaconfigs.api.common.utils.string.util.VersionDiff;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.PluginManager;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Handler;

import static eu.locklogin.plugin.bungee.LockLogin.*;

public class BungeeManager {

    private static CommandSender update_issuer = null;

    /**
     * Unload LockLogin plugin
     */
    @SuppressWarnings("all")
    public static void unload() {
        IllegalStateException error = new IllegalStateException("Errors occurred while unloading plugin " + plugin.getDescription().getName()) {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        };

        PluginManager pluginmanager = ProxyServer.getInstance().getPluginManager();
        ClassLoader pluginclassloader = plugin.getClass().getClassLoader();

        try {
            plugin.onDisable();
        } catch (Throwable t) {
            error.addSuppressed(t);
        }

        try {
            for (Handler handler : plugin.getLogger().getHandlers()) {
                handler.close();
            }
        } catch (Throwable t) {
            error.addSuppressed(t);
        }

        try {
            pluginmanager.unregisterListeners(plugin);
        } catch (Throwable t) {
            error.addSuppressed(t);
        }

        try {
            pluginmanager.unregisterCommands(plugin);
        } catch (Throwable t) {
            error.addSuppressed(t);
        }

        try {
            ProxyServer.getInstance().getScheduler().cancel(plugin);
        } catch (Throwable t) {
            error.addSuppressed(t);
        }

        try {
            plugin.getExecutorService().shutdownNow();
        } catch (Throwable t) {
            error.addSuppressed(t);
        }

        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getClass().getClassLoader() == pluginclassloader) {
                try {
                    thread.interrupt();
                    thread.join(2000);
                    if (thread.isAlive()) {
                        thread.stop();
                    }
                } catch (Throwable t) {
                    error.addSuppressed(t);
                }
            }
        }

        EventBusManager.completeIntents(plugin);

        try {
            Map<String, Command> commandMap = Reflections.getFieldValue(pluginmanager, "commandMap");
            commandMap.entrySet().removeIf(entry -> entry.getValue().getClass().getClassLoader() == pluginclassloader);
        } catch (Throwable t) {
            error.addSuppressed(t);
        }

        try {
            Reflections.<Map<String, Plugin>>getFieldValue(pluginmanager, "plugins").values().remove(plugin);
        } catch (Throwable t) {
            error.addSuppressed(t);
        }

        if (pluginclassloader instanceof URLClassLoader) {
            try {
                ((URLClassLoader) pluginclassloader).close();
            } catch (Throwable t) {
                error.addSuppressed(t);
            }
        }

        try {
            Reflections.<Set<ClassLoader>>getStaticFieldValue(pluginclassloader.getClass(), "allLoaders").remove(pluginclassloader);
        } catch (Throwable t) {
            error.addSuppressed(t);
        }

        if (error.getSuppressed().length > 0) {
            error.printStackTrace();
        }
    }

    /**
     * Load LockLogin plugin
     *
     * @param file the plugin file
     */
    private static void load(final File file) {
        ProxyServer proxyserver = ProxyServer.getInstance();
        PluginManager pluginmanager = proxyserver.getPluginManager();

        try (JarFile jar = new JarFile(file)) {
            JarEntry pdf = jar.getJarEntry("bungee.yml");
            if (pdf == null) {
                pdf = jar.getJarEntry("plugin.yml");
            }
            try (InputStream in = jar.getInputStream(pdf)) {
                PluginDescription desc = new Yaml().loadAs(in, PluginDescription.class);
                desc.setFile(file);
                HashSet<String> plugins = new HashSet<>();
                for (Plugin plugin : pluginmanager.getPlugins()) {
                    plugins.add(plugin.getDescription().getName());
                }
                for (String dependency : desc.getDepends()) {
                    if (!plugins.contains(dependency)) {
                        throw new IllegalArgumentException(MessageFormat.format("Missing plugin dependency {0}", dependency));
                    }
                }
                Plugin plugin = (Plugin)
                        Reflections.setAccessible(
                                        Main.class.getClassLoader().getClass()
                                                .getDeclaredConstructor(ProxyServer.class, PluginDescription.class, URL[].class)
                                )
                                .newInstance(proxyserver, desc, new URL[]{file.toURI().toURL()})
                                .loadClass(desc.getMain()).getDeclaredConstructor()
                                .newInstance();
                Reflections.invokeMethod(plugin, "init", proxyserver, desc);
                Reflections.<Map<String, Plugin>>getFieldValue(pluginmanager, "plugins").put(desc.getName(), plugin);

                plugin.onEnable();
            }
        } catch (Throwable t) {
            t.printStackTrace();
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

                        VersionComparator comparator = StringUtils.compareTo(VersionComparator.createBuilder()
                                .currentVersion(current).checkVersion(version));
                        if (comparator.getDifference().equals(VersionDiff.OVERDATED)) {
                            major_files.add(file);
                        } else {
                            try {
                                Files.deleteIfExists(file.toPath());
                            } catch (Throwable ignored) {
                            }
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

                            VersionComparator comparator = StringUtils.compareTo(VersionComparator.createBuilder()
                                    .currentVersion(current_major_version).checkVersion(latest_major_version));
                            if (comparator.getDifference().equals(VersionDiff.OVERDATED)) {
                                try {
                                    Files.deleteIfExists(latest_major.toPath());
                                } catch (Throwable ignored) {
                                }
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
            File curr_jar = new File(FileUtilities.getProjectFolder("plugins"), lockloginFile.getName());

            if (last_jar != null) {
                UpdateChannel current_channel = config.getUpdaterOptions().getChannel();
                UpdateChannel update_channel = FileInfo.getChannel(last_jar);

                //Make sure the latest jar version id is not the current one
                switch (current_channel) {
                    case RELEASE:
                        if (update_channel.equals(UpdateChannel.RELEASE)) {
                            update(last_jar, curr_jar);
                        } else {
                            update_issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "update channel is over the current one", "no second reason"))));
                        }
                        break;
                    case RC:
                        switch (update_channel) {
                            case RELEASE:
                            case RC:
                                update(last_jar, curr_jar);
                                break;
                            default:
                                update_issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "update channel is over the current one", "no second reason"))));
                                break;
                        }
                    case SNAPSHOT:
                        update(last_jar, curr_jar);
                        break;
                    default:
                        update_issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "unknown update channel", "no second reason"))));
                        break;
                }
            } else {
                update_issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file is null or jar version is lower than current )")))));
            }

            update_issuer = null;
        } else {
            if (update_issuer != issuer)
                issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_already_updating", "&5&oUpdate process is already being processed by {0}"), update_issuer.getName()))));
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

        eu.locklogin.api.module.plugin.api.event.util.Event update_start = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UPDATE_START, null);
        ModulePlugin.callEvent(update_start);

        Timer load_timer = new Timer();
        load_timer.schedule(new TimerTask() {
            int back = 8;

            @Override
            public void run() {
                if (back != 0 && back <= 5) {
                    issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_loading", "&dLoading new plugin instance in {0} seconds"), back))));
                } else {
                    if (back == 0) {
                        load_timer.cancel();
                        try {
                            Files.move(update_jar.toPath(), current_jar.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_moved", "&dMoved new plugin instance, replacing the current one..."))));
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
        issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_unloaded", "&dUnloaded current LockLogin instance and prepared new instance..."))));
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
