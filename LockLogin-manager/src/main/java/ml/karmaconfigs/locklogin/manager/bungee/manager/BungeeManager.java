package ml.karmaconfigs.locklogin.manager.bungee.manager;

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
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.modules.api.event.plugin.PluginStatusChangeEvent;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.utils.enums.UpdateChannel;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.bungee.Main;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.Message;
import ml.karmaconfigs.locklogin.plugin.bungee.util.files.data.RestartCache;
import ml.karmaconfigs.locklogin.plugin.common.utils.FileInfo;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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

import static ml.karmaconfigs.locklogin.plugin.bungee.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.bungee.permissibles.PluginPermission.applyUnsafeUpdates;

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

        if (update_issuer instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) update_issuer;
            if (!player.isConnected())
                update_issuer = issuer;
        }

        if (update_issuer == null && update_issuer != issuer) {
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
                        if (update_issuer instanceof ProxiedPlayer) {
                            if (unsafe) {
                                versionCriteria = true;
                            } else {
                                update_issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + messages.permissionError(applyUnsafeUpdates()))));
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
                                update_issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "update channel is over the current one", "no second reason"))));
                            }
                            break;
                        case RC:
                            switch (update_channel) {
                                case RELEASE:
                                case RC:
                                    update(last_jar.getFile(), curr_jar);
                                    break;
                                default:
                                    update_issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "update channel is over the current one", "no second reason"))));
                                    break;
                            }
                        case SNAPSHOT:
                            update(last_jar.getFile(), curr_jar);
                            break;
                        default:
                            update_issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "unknown update channel", "no second reason"))));
                    }
                } else {
                    update_issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"),
                            !last_jar.getVersion().equals(versionID), versionCriteria))));
                }
            } else {
                update_issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_not_available", "&5&oUpdate process is not available ( jar file could not be found, or version is lower and unsafe updates are disabled )"), "jar file is null", "no second reason"))));
            }

            update_issuer = null;
        } else {
            if (update_issuer != null && update_issuer != issuer)
                issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_already_updating", "&5&oUpdate process is already being processed by {0}"), update_issuer.getName()))));
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
                    issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + StringUtils.formatString(properties.getProperty("updater_loading", "&dLoading new plugin instance in {0} seconds"), back))));
                } else {
                    if (back == 0) {
                        load_timer.cancel();
                        try {
                            Files.move(update_jar.toPath(), current_jar.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            issuer.sendMessage(TextComponent.fromLegacyText(StringUtils.toColor(messages.prefix() + properties.getProperty("updater_moved", "&dMoved new plugin instance, replacing the current one..."))));
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
