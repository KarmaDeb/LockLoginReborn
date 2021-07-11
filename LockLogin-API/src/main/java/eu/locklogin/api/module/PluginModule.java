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

import eu.locklogin.api.module.plugin.javamodule.console.ModuleConsole;
import eu.locklogin.api.module.plugin.javamodule.ModuleScheduler;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.loader.JarAppender;
import ml.karmaconfigs.api.common.karma.loader.KarmaBootstrap;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.api.common.utils.StringUtils;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * LockLogin plugin module
 */
public abstract class PluginModule implements KarmaSource, KarmaBootstrap {

    private static boolean timerAutoStart = true;
    private static JarAppender appender;

    /**
     * Initialize the plugin module
     *
     * @throws IllegalAccessError if the module
     * is already initialized
     */
    public PluginModule() throws IllegalAccessError {
        if (ModuleLoader.isLoaded(name())) {
            throw new IllegalAccessError("Tried to initialize module " + name() + " but it's already initialized!");
        }
    }

    @SafeVarargs
    public <T> PluginModule(final T... constructorParams) {
        if (ModuleLoader.isLoaded(name())) {
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
     * Set if the timers start automatically
     *
     * @param auto timer auto start
     */
    public final void setTimerAutoStart(final boolean auto) {
        timerAutoStart = auto;
    }

    /**
     * Get if the timer starts automatically
     *
     * @return if the timer starts automatically
     */
    public final boolean isTimerAutoStart() {
        return timerAutoStart;
    }

    /**
     * Reload the module
     */
    public final void reload() {
        unload();
        load();
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
            File modulesFolder = new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "plugin", "modules");

            ModuleLoader loader = new ModuleLoader(modulesFolder);

            PluginModule module = ModuleLoader.getByName(name());
            if (module != null) {
                if (ModuleLoader.isLoaded(module.name())) {
                    loader.unloadModule(module.name());
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
            File modulesFolder = new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "plugin", "modules");

            ModuleLoader loader = new ModuleLoader(modulesFolder);

            PluginModule module = ModuleLoader.getByName(name());
            if (module != null) {
                if (!ModuleLoader.isLoaded(module.name())) {
                    loader.loadModule(module.name());

                    return true;
                }
            }
        } catch (Throwable ignored) {}

        return false;
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
        } catch (Throwable ignored) {
        }

        return url;
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
     * Stop the running tasks
     */
    @Override
    public final void stopTasks() {
        KarmaSource.super.stopTasks();
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
     * Get the module scheduler
     *
     * @return the module scheduler
     */
    public final ModuleScheduler getScheduler() {
        return new ModuleScheduler(this);
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
