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
import ml.karmaconfigs.api.common.data.file.FileUtilities;
import ml.karmaconfigs.api.common.data.path.PathUtilities;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.version.comparator.VersionComparator;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.PluginManager;
import org.yaml.snakeyaml.Yaml;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Handler;
import java.util.stream.Stream;

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
        AtomicReference<File> update_file = new AtomicReference<>(null);

        Path update_folder = PathUtilities.getFixedPath(plugin.getDataPath()).resolve("plugin").resolve("updater");
        String latest_version = versionID.resolve(version);
        if (Files.exists(update_folder) && Files.isDirectory(update_folder)) {
            try {
                try(Stream<Path> files = Files.list(update_folder)) {
                    files.forEachOrdered((downloaded) -> {
                        String name = PathUtilities.getName(downloaded, true);
                        String extension = PathUtilities.getExtension(downloaded);
                        if (extension.equalsIgnoreCase("jar")) {
                            //We only want jar files here!
                            JarFile jar = null;
                            InputStream data = null;
                            try {
                                jar = new JarFile(downloaded.toFile());
                                JarEntry entry = jar.getJarEntry("global.yml");

                                if (entry != null) {
                                    data = jar.getInputStream(entry);
                                    KarmaYamlManager manager = new KarmaYamlManager(data);

                                    String target_version = manager.getString("project_version", null);
                                    if (!StringUtils.isNullOrEmpty(target_version)) {
                                        VersionComparator comparator = new VersionComparator(
                                                VersionComparator.createBuilder()
                                                        .currentVersion(target_version)
                                                        .checkVersion(latest_version)
                                        );

                                        if (comparator.isUpToDate()) {
                                            File curr = update_file.get();
                                            if (curr != null)
                                                FileUtilities.destroy(curr);

                                            update_file.set(downloaded.toFile());
                                        } else {
                                            PathUtilities.destroy(downloaded);
                                        }
                                    }
                                }
                            } catch (Throwable ex) {
                                plugin.logger().scheduleLog(Level.GRAVE, ex);
                                plugin.logger().scheduleLog(Level.INFO, "Failed to read POSSIBLE LockLogin update jar {0}", name);

                                plugin.console().send("An error occurred while trying to read jar file {0}. This may cause the plugin to not update", Level.INFO, name);
                            } finally {
                                tryClose(jar);
                                tryClose(data);
                            }
                        }
                    });
                }
            } catch (Throwable ex) {
                plugin.logger().scheduleLog(Level.GRAVE, ex);
                plugin.logger().scheduleLog(Level.INFO, "Failed to update LockLogin");

                plugin.console().send("An internal error occurred while trying to update LockLogin. More information at logs", Level.GRAVE);
            }
        }

        return update_file.get();
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
                            plugin.logger().scheduleLog(Level.GRAVE, ex);
                            plugin.logger().scheduleLog(Level.INFO, "Failed to update LockLogin");

                            plugin.console().send("An internal error occurred while trying to update LockLogin. RESTARTING THE SERVER MAY TRIGGER THE UPDATE. More information at logs", Level.GRAVE);
                            try {
                                Files.copy(update_jar.toPath(), current_jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            } catch (Throwable ignored) {}
                        }
                    }
                }
                back--;
            }
        }, 0, 1000);

        unload();
        issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_unloaded", "&dUnloaded current LockLogin instance and prepared new instance..."))));
    }

    /**
     * Tries to close a closeable object
     *
     * @param closeable the closeable object
     */
    private static void tryClose(final Closeable closeable) {
        try {
            if (closeable != null)
                closeable.close();
        } catch (Throwable ignored) {}
    }
}
