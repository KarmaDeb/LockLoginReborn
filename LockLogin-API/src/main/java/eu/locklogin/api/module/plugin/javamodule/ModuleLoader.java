package eu.locklogin.api.module.plugin.javamodule;

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

import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.api.event.plugin.ModuleStatusChangeEvent;
import ml.karmaconfigs.api.common.karma.loader.JarAppender;
import ml.karmaconfigs.api.common.utils.StringUtils;
import eu.locklogin.api.util.platform.CurrentPlatform;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * LockLogin java module loader
 */
public final class ModuleLoader {

    private final static Set<PluginModule> loaded = new LinkedHashSet<>();

    private final static Map<Class<? extends PluginModule>, PluginModule> clazz_map = new ConcurrentHashMap<>();

    private final static Set<String> load_queue = new LinkedHashSet<>();

    private static File modulesFolder = null;

    /**
     * Initialize the loader
     *
     * @param modules the modules folder
     */
    public ModuleLoader(final File modules) {
        modulesFolder = modules;
    }

    /**
     * Check if the module is loaded
     *
     * @param name the module name
     * @return if the module is loaded
     */
    public static boolean isLoaded(final String name) {
        for (PluginModule module : loaded) {
            if (module.name().equalsIgnoreCase(StringUtils.replaceLast(name, ".jar", "")))
                return true;
        }

        return false;
    }

    /**
     * Check if the module is loaded
     *
     * @param module the module
     * @return if the module is loaded
     */
    public static boolean isLoaded(final PluginModule module) {
        for (PluginModule mod : loaded) {
            if (mod.name().equalsIgnoreCase(StringUtils.replaceLast(module.name(), ".jar", "")))
                return true;
        }

        return false;
    }

    /**
     * Get a plugin module by name
     *
     * @param name the plugin module name
     * @return the plugin module
     */
    @Nullable
    public static PluginModule getByName(final String name) {
        for (PluginModule module : loaded) {
            if (module.name().equalsIgnoreCase(StringUtils.replaceLast(name, ".jar", "")))
                return module;
        }

        if (modulesFolder != null) {
            File[] files = modulesFolder.listFiles();

            if (files != null) {
                if (!isLoaded(name)) {
                    for (File moduleFile : files) {
                        if (moduleFile.isFile()) {
                            if (!load_queue.contains(moduleFile.getName())) {
                                try {
                                    JarFile jar = new JarFile(moduleFile);

                                    ZipEntry module_yml = jar.getEntry("module.yml");
                                    if (module_yml != null) {
                                        InputStream module_stream = jar.getInputStream(module_yml);
                                        if (module_stream != null) {
                                            Yaml yaml = new Yaml();
                                            Map<String, Object> values = yaml.load(module_stream);

                                            String module_name = values.getOrDefault("name", "").toString();
                                            if (module_name.equalsIgnoreCase(name) || moduleFile.getName().replace(".jar", "").equalsIgnoreCase(name.replace(".jar", ""))) {
                                                String class_name = null;
                                                Class<?> main = CurrentPlatform.getMain();

                                                switch (CurrentPlatform.getPlatform()) {
                                                    case BUKKIT:
                                                        class_name = values.getOrDefault("loader_bukkit", "").toString();
                                                        break;
                                                    case BUNGEE:
                                                        class_name = values.getOrDefault("loader_bungee", "").toString();
                                                        break;
                                                    case VELOCITY:
                                                        class_name = values.getOrDefault("loader_velocity", "").toString();
                                                        break;
                                                }

                                                if (class_name != null && !class_name.replaceAll("\\s", "").isEmpty()) {
                                                    JarAppender appender = CurrentPlatform.getPluginAppender();
                                                    appender.addJarToClasspath(moduleFile);

                                                    Class<? extends PluginModule> module_class;
                                                    boolean pluginAppender = false;
                                                    if (appender.getLoader() != null) {
                                                        module_class = appender.getLoader().loadClass(class_name).asSubclass(PluginModule.class);
                                                    } else {
                                                        URLClassLoader loader = new URLClassLoader(
                                                                new URL[]{new URL("file:///" + moduleFile.getAbsolutePath().replaceAll("%20", " "))}, main.getClassLoader());

                                                        module_class = loader.loadClass(class_name).asSubclass(PluginModule.class);
                                                        pluginAppender = true;
                                                    }

                                                    CurrentPlatform.getPluginAppender().addJarToClasspath(moduleFile);

                                                    PluginModule module = module_class.getDeclaredConstructor().newInstance();

                                                    if (pluginAppender)
                                                        module.setAppender(CurrentPlatform.getPluginAppender());

                                                    module.getAppender().addJarToClasspath(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation());

                                                    //Tries to fix problems when loading/unloading modules using /locklogin modules load/unload/reload
                                                    CurrentPlatform.getPluginAppender().addJarToClasspath(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation());
                                                    return module;
                                                }
                                            }
                                        }
                                    }

                                    jar.close();
                                } catch (Throwable ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    throw new IllegalStateException("Tried to load module " + name + " but it's already loaded!");
                }
            }
        }

        return null;
    }

    /**
     * Get a module by its module class
     *
     * @param clazz the module class
     * @return the plugin module attached
     * to that module clazz
     */
    @Nullable
    public static PluginModule getByModuleClazz(final Class<? extends PluginModule> clazz) {
        return clazz_map.getOrDefault(clazz, null);
    }

    /**
     * Get a list of the loaded modules
     *
     * @return a list of the loaded modules
     */
    public static Set<PluginModule> getModules() {
        return new LinkedHashSet<>(loaded);
    }

    /**
     * Load the specified module
     *
     * @param name the module name
     */
    public final void loadModule(final String name) throws IllegalStateException {
        if (modulesFolder != null) {
            File[] files = modulesFolder.listFiles();

            if (files != null) {
                if (!isLoaded(name)) {
                    for (File moduleFile : files) {
                        if (moduleFile.isFile()) {
                            if (!load_queue.contains(moduleFile.getName())) {
                                try {
                                    JarFile jar = new JarFile(moduleFile);

                                    ZipEntry module_yml = jar.getEntry("module.yml");
                                    if (module_yml != null) {
                                        InputStream module_stream = jar.getInputStream(module_yml);
                                        if (module_stream != null) {
                                            Yaml yaml = new Yaml();
                                            Map<String, Object> values = yaml.load(module_stream);

                                            String module_name = values.getOrDefault("name", "").toString();
                                            if (module_name.equalsIgnoreCase(name) || moduleFile.getName().replace(".jar", "").equalsIgnoreCase(name.replace(".jar", ""))) {
                                                load_queue.add(moduleFile.getName());

                                                String class_name = null;
                                                Class<?> main = CurrentPlatform.getMain();

                                                switch (CurrentPlatform.getPlatform()) {
                                                    case BUKKIT:
                                                        class_name = values.getOrDefault("loader_bukkit", "").toString();
                                                        break;
                                                    case BUNGEE:
                                                        class_name = values.getOrDefault("loader_bungee", "").toString();
                                                        break;
                                                    case VELOCITY:
                                                        class_name = values.getOrDefault("loader_velocity", "").toString();
                                                        break;
                                                }

                                                if (class_name != null && !class_name.replaceAll("\\s", "").isEmpty()) {
                                                    JarAppender appender = CurrentPlatform.getPluginAppender();
                                                    appender.addJarToClasspath(moduleFile);

                                                    Class<? extends PluginModule> module_class;
                                                    boolean pluginAppender = false;
                                                    if (appender.getLoader() != null) {
                                                       module_class = appender.getLoader().loadClass(class_name).asSubclass(PluginModule.class);
                                                    } else {
                                                        URLClassLoader loader = new URLClassLoader(
                                                                new URL[]{new URL("file:///" + moduleFile.getAbsolutePath().replaceAll("%20", " "))}, main.getClassLoader());

                                                        module_class = loader.loadClass(class_name).asSubclass(PluginModule.class);
                                                        pluginAppender = true;
                                                    }

                                                    CurrentPlatform.getPluginAppender().addJarToClasspath(moduleFile);

                                                    PluginModule module = module_class.getDeclaredConstructor().newInstance();
                                                    loaded.add(module);

                                                    if (pluginAppender)
                                                        module.setAppender(CurrentPlatform.getPluginAppender());

                                                    module.getAppender().addJarToClasspath(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation());

                                                    ModuleStatusChangeEvent event = new ModuleStatusChangeEvent(ModuleStatusChangeEvent.Status.LOAD, module, this, null);
                                                    ModulePlugin.callEvent(event);

                                                    clazz_map.put(module_class, module);

                                                    module.enable();
                                                    //Perform the first updater is enabled check
                                                    module.getPlugin().getVersionManager().updaterEnabled();
                                                    load_queue.remove(moduleFile.getName());

                                                    //Tries to fix problems when loading/unloading modules using /locklogin modules load/unload/reload
                                                    CurrentPlatform.getPluginAppender().addJarToClasspath(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation());
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                } catch (Throwable ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    throw new IllegalStateException("Tried to load module " + name + " but it's already loaded!");
                }
            }
        }
    }

    /**
     * Unload the specified module
     *
     * @param name the module name
     */
    public final void unloadModule(final String name) throws IllegalArgumentException {
        if (isLoaded(name)) {
            PluginModule module = getByName(name);
            if (module != null) {
                module.getPlugin().unregisterListeners();
                module.getPlugin().unregisterCommands();

                module.stopTasks();
                module.disable();
                module.getAppender().close();
                loaded.remove(module);
                clazz_map.remove(module.getClass());
            } else {
                throw new IllegalArgumentException("Tried to unload module " + name + " but it's not loaded!");
            }
        }
    }

    /**
     * Get the modules folder
     *
     * @return the modules folder
     */
    public final File getDataFolder() {
        return modulesFolder;
    }
}