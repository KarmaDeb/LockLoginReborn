package eu.locklogin.api.module;

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

import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.ModuleScheduler;
import eu.locklogin.api.module.plugin.javamodule.sender.ModuleConsole;
import eu.locklogin.api.module.plugin.javamodule.updater.JavaModuleVersion;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.ModuleServer;
import ml.karmaconfigs.api.common.Logger;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.loader.JarAppender;
import ml.karmaconfigs.api.common.karma.loader.KarmaBootstrap;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.karmafile.karmayaml.FileCopy;
import ml.karmaconfigs.api.common.karmafile.karmayaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * LockLogin plugin module
 */
@SuppressWarnings("unused")
public abstract class PluginModule implements KarmaSource, KarmaBootstrap {

    private JarAppender appender;

    /**
     * Initialize the plugin module
     *
     * @throws IllegalAccessError if the module
     * is already initialized
     */
    public PluginModule() throws IllegalAccessError {
        if (ModuleLoader.isLoaded(this)) {
            throw new IllegalAccessError("Tried to initialize module " + name() + " but it's already initialized!");
        }
    }

    /**
     * Get a module by its main class
     *
     * @param clazz the main module clazz
     * @return the module
     */
    @Nullable
    public static Class<? extends PluginModule> getModuleClass(final Class<?> clazz) {
        if (PluginModule.class.isAssignableFrom(clazz)) {
            return clazz.asSubclass(PluginModule.class);
        } else {
            return null;
        }
    }

    /**
     * On module enable logic
     */
    @Override
    public abstract void enable();

    /**
     * On module disable logic
     */
    @Override
    public abstract void disable();

    /**
     * Set the module appender, very util for velocity
     * module or if the developer has his own
     * jar appender
     *
     * @param newAppender the new appender
     */
    public final void setAppender(final JarAppender newAppender) {
        appender = newAppender;
    }

    /**
     * Reload the module
     */
    public final void reload() {
        unload();
        load();
    }

    /**
     * Log something into the plugin's logger
     *
     * @param level the log level
     * @param info the info to log
     */
    public final void log(final Level level, final Object info) {
        Logger logger = new Logger(getSource());

        if (info instanceof Throwable) {
            Throwable error = (Throwable) info;
            logger.scheduleLog(level, error);
        } else {
            try {
                logger.scheduleLog(level, info.toString());
            } catch (Throwable ex) {
                logger.scheduleLog(level, String.valueOf(info));
            }
        }
    }

    /**
     * Get the KarmaSource
     *
     * @return the karma source
     */
    @Override
    public final KarmaSource getSource() {
        return this;
    }

    @Override
    public final JarAppender getAppender() {
        if (appender == null)
            return KarmaBootstrap.super.getAppender();
        else
            return appender;
    }

    /**
     * Unload the module
     *
     * @return if the module could be unloaded
     */
    public final boolean unload() {
        try {
            ModuleLoader loader = new ModuleLoader();

            PluginModule module = ModuleLoader.getByFile(getModule());
            if (module != null) {
                if (ModuleLoader.isLoaded(module)) {
                    loader.unloadModule(getModule());
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    /**
     * Load the module
     *
     * @return if the module could be loaded
     */
    public final boolean load() {
        try {
            ModuleLoader loader = new ModuleLoader();

            PluginModule module = ModuleLoader.getByFile(getModule());
            if (!ModuleLoader.isLoaded(module)) {
                loader.loadModule(getModule(), loadRule());

                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    /**
     * Export the resource
     *
     * @param resource the resource to export
     * @return if the resource could be exported
     */
    public final boolean export(final String resource, final Path destination) {
        try {
            FileCopy copy = new FileCopy(this, resource);
            copy.copy(FileUtilities.getFixedFile(destination.toFile()));

            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Export the resource
     *
     * @param resource the resource to export
     * @return if the resource could be exported
     */
    public final boolean export(final String resource, final File destination) {
        try {
            FileCopy copy = new FileCopy(this, resource);
            copy.copy(FileUtilities.getFixedFile(destination));

            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Get the module name
     *
     * @return the module name
     */
    @Override
    public final @NotNull String name() {
        String name = "name not set in module.yml";

        try {
            String flName = new File(this.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()).getName();

            JarFile jar = new JarFile(new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
            ZipEntry moduleEntry = jar.getEntry("module.yml");

            if (moduleEntry != null) {
                InputStream module_yml = jar.getInputStream(moduleEntry);
                if (module_yml != null) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> values = yaml.load(module_yml);

                    name = values.getOrDefault("name", "name not set in module.yml").toString();
                    module_yml.close();
                }
            }

            jar.close();
        } catch (Throwable ignored) {
        }

        return name;
    }

    /**
     * Get the module version
     *
     * @return the module version
     */
    @Override
    public final @NotNull String version() {
        String version = "version not set in module.yml";

        try {
            String flName = new java.io.File(this.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()).getName();

            JarFile jar = new JarFile(new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
            ZipEntry moduleEntry = jar.getEntry("module.yml");

            if (moduleEntry != null) {
                InputStream module_yml = jar.getInputStream(moduleEntry);
                if (module_yml != null) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> values = yaml.load(module_yml);

                    version = values.getOrDefault("version", "version not set in module.yml").toString();
                    module_yml.close();
                }
            }

            jar.close();
        } catch (Throwable ignored) {
        }

        return version;
    }

    /**
     * Get the module description
     * as string
     *
     * @return the module description as string
     */
    @Override
    public @NotNull String description() {
        String description = "LockLogin module ( " + this.getClass().getPackage().getName() + " )";

        try {
            String flName = new java.io.File(this.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()).getName();

            JarFile jar = new JarFile(new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
            ZipEntry moduleEntry = jar.getEntry("module.yml");

            if (moduleEntry != null) {
                InputStream module_yml = jar.getInputStream(moduleEntry);
                if (module_yml != null) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> values = yaml.load(module_yml);

                    Object desc = values.getOrDefault("description", "LockLogin module ( " + this.getClass().getPackage().getName() + " )");
                    if (desc instanceof List) {
                        List<?> list = (List<?>) desc;
                        StringBuilder descBuilder = new StringBuilder();

                        for (Object obj : list) {
                            descBuilder.append(obj).append(" ");
                        }

                        description = StringUtils.replaceLast(descBuilder.toString(), " ", "");
                    } else {
                        if (desc.getClass().isArray()) {
                            Object[] array = (Object[]) desc;

                            StringBuilder descBuilder = new StringBuilder();

                            for (Object obj : array) {
                                descBuilder.append(obj).append(" ");
                            }

                            description = StringUtils.replaceLast(descBuilder.toString(), " ", "");
                        } else {
                            description = desc.toString();
                        }
                    }

                    module_yml.close();
                }
            }

            jar.close();
        } catch (Throwable ignored) {
        }

        return description;
    }

    /**
     * Get the module name
     *
     * @return the module name
     */
    public final @NotNull LoadRule loadRule() {
        String rule = "POSTPLUGIN";

        try {
            String flName = new File(this.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()).getName();

            JarFile jar = new JarFile(new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
            ZipEntry moduleEntry = jar.getEntry("module.yml");

            if (moduleEntry != null) {
                InputStream module_yml = jar.getInputStream(moduleEntry);
                if (module_yml != null) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> values = yaml.load(module_yml);

                    rule = values.getOrDefault("load", "POSTPLUGIN").toString();
                    module_yml.close();
                }
            }

            jar.close();
        } catch (Throwable ignored) {
        }

        try {
            return LoadRule.valueOf(rule.toUpperCase());
        } catch (Throwable ex) {
            return LoadRule.PREPLUGIN;
        }
    }

    /**
     * Get the module auth
     *
     * @return the module author
     */
    @Override
    public @NotNull String[] authors() {
        String[] author = new String[]{"KarmaDev"};

        try {
            String flName = new java.io.File(this.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()).getName();

            JarFile jar = new JarFile(new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
            ZipEntry moduleEntry = jar.getEntry("module.yml");

            if (moduleEntry != null) {
                InputStream module_yml = jar.getInputStream(moduleEntry);
                if (module_yml != null) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> values = yaml.load(module_yml);

                    Object desc = values.getOrDefault("authors", "KarmaDev");
                    if (desc instanceof List) {
                        List<?> list = (List<?>) desc;
                        StringBuilder descBuilder = new StringBuilder();

                        for (Object obj : list) {
                            descBuilder.append(obj).append("-");
                        }

                        author = StringUtils.replaceLast(descBuilder.toString(), "-", "").split("-");
                    } else {
                        if (desc.getClass().isArray()) {
                            Object[] array = (Object[]) desc;

                            StringBuilder descBuilder = new StringBuilder();

                            for (Object obj : array) {
                                descBuilder.append(obj).append("-");
                            }

                            author = StringUtils.replaceLast(descBuilder.toString(), "-", "").split("-");
                        } else {
                            author = new String[]{desc.toString()};
                        }
                    }

                    module_yml.close();
                }
            }

            jar.close();
        } catch (Throwable ignored) {
        }

        return author;
    }

    /**
     * Get the authors in a single string
     * spaced with commas
     *
     * @return the authors in a single string
     */
    public final String singleAuthors() {
        StringBuilder builder = new StringBuilder();

        for (String author : authors())
            builder.append(author).append(", ");

        return StringUtils.replaceLast(builder.toString(), ", ", "");
    }

    /**
     * Get the authors in a single string
     * spaced with the specified char
     *
     * @param spaceChar the space character
     * @return the authors in a single string
     */
    public final String singleAuthors(final char spaceChar) {
        StringBuilder builder = new StringBuilder();

        String space = spaceChar + " ";
        for (String author : authors())
            builder.append(author).append(space);

        return StringUtils.replaceLast(builder.toString(), space, "");
    }

    /**
     * Get module update url
     *
     * @return the module update url
     */
    @Override
    public @NotNull String updateURL() {
        String url = "";

        try {
            String flName = new java.io.File(this.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()).getName();

            JarFile jar = new JarFile(new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
            ZipEntry moduleEntry = jar.getEntry("module.yml");

            if (moduleEntry != null) {
                InputStream module_yml = jar.getInputStream(moduleEntry);
                if (module_yml != null) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> values = yaml.load(module_yml);

                    url = values.getOrDefault("update_url", "").toString();
                    module_yml.close();
                }
            }

            jar.close();
        } catch (Throwable ignored) {}
        if (!url.endsWith(".txt")) {
            if (url.endsWith("/")) {
                url = url + "latest.txt";
            } else {
                url = url + "/latest.txt";
            }
        }

        return url.replaceAll("\\s", "%20");
    }

    /**
     * Get an internal resource from the module
     *
     * @param name the resource name
     * @param dirs the resource location
     * @return the resource stream
     */
    public final InputStream getResource(final String name, final String... dirs) {
        if (dirs.length > 0) {
            StringBuilder dirBuilder = new StringBuilder();
            for (String str : dirs)
                dirBuilder.append(str).append("/");

            return getClass().getResourceAsStream(dirBuilder + name);
        } else {
            return getClass().getResourceAsStream("/" + name);
        }
    }

    /**
     * Get the module data folder
     *
     * @return the module data folder
     */
    public final File getDataFolder() {
        return new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + File.separator + "plugin" + File.separator + "modules", this.name());
    }

    /**
     * Get the module data path
     *
     * @return the module data path
     */
    @Override
    public final Path getDataPath() {
        return getDataFolder().toPath();
    }

    /**
     * Get the LockLogin plugin file
     *
     * @return the LockLogin plugin file
     */
    public final File getLockLogin() {
        return new File(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " "));
    }

    /**
     * Get the module file
     *
     * @return the module file
     */
    public final File getModule() {
        return new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " "));
    }

    /**
     * Get the module file
     *
     * @param name the file name
     * @param path the file sub-directory
     * @return the module file
     */
    public final File getFile(final String name, final String... path) {
        if (path.length > 0) {
            StringBuilder path_builder = new StringBuilder();
            for (String sub_path : path)
                path_builder.append(File.separator).append(sub_path);

            return new File(getDataFolder().getAbsolutePath().replace("%20", " ") + path_builder, name);
        } else {
            return new File(getDataFolder(), name);
        }
    }

    /**
     * Load a yaml file
     *
     * @param name the yaml file name
     * @param path the yaml file path
     * @return the yaml file manager
     */
    public final KarmaYamlManager loadYaml(final String name, final String... path) {
        File file = getFile(name, path);
        HashMap<Object, String> defaults = new HashMap<>();

        if (file.exists()) {
            try {
                return new KarmaYamlManager(file);
            } catch (Throwable ex) {
                return new KarmaYamlManager(defaults);
            }
        } else {
            return new KarmaYamlManager(defaults);
        }
    }

    /**
     * Load a karma file
     *
     * @param name the karma file name
     * @param path the karma file path
     * @return the karma file
     */
    public final KarmaFile loadFile(final String name, final String... path) {
        return new KarmaFile(this, name, path);
    }

    /**
     * Get the module manager
     *
     * @return the module manager
     */
    public final ModulePlugin getPlugin() {
        return new ModulePlugin(this);
    }

    /**
     * Get the console sender
     *
     * @return the module console sender
     */
    public final ModuleConsole getConsole() {
        return new ModuleConsole(this);
    }

    /**
     * Get the module server
     *
     * @return the module sender
     */
    public final ModuleServer getServer() {
        return CurrentPlatform.getServer();
    }

    /**
     * Get the module updater
     *
     * @return the module updater
     */
    public final JavaModuleVersion getUpdater() {
        return getPlugin().getVersionManager();
    }

    /**
     * Get the module scheduler
     *
     * @return the module scheduler
     */
    public final ModuleScheduler getScheduler() {
        return new ModuleScheduler(this, 1, false);
    }

    /**
     * Get the module scheduler
     *
     * @param time the scheduler time before ending
     * @return the module scheduler
     */
    public final ModuleScheduler getScheduler(final Number time) {
        return new ModuleScheduler(this, time, false);
    }

    /**
     * Get the module scheduler
     *
     * @param time the scheduler time before restarting
     * @return the module scheduler
     */
    public final ModuleScheduler getRepeatingScheduler(final Number time) {
        return new ModuleScheduler(this, time, true);
    }

    /**
     * Get this module ID
     *
     * @return the module ID
     */
    public final UUID getID() {
        return UUID.nameUUIDFromBytes(("PluginModule: " + this.getClass().getName()).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get if the module is the same as the
     * specified object
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof PluginModule) {
            PluginModule module = (PluginModule) obj;
            return module.getID().equals(getID());
        }

        return false;
    }
}
