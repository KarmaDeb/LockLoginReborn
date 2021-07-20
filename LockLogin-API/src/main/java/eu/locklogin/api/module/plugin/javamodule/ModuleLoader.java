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

import eu.locklogin.api.module.LoadRule;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.api.event.plugin.ModuleStatusChangeEvent;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.Console;
import ml.karmaconfigs.api.common.karma.loader.JarAppender;
import ml.karmaconfigs.api.common.karma.loader.KarmaBootstrap;
import ml.karmaconfigs.api.common.karma.loader.SubJarLoader;
import ml.karmaconfigs.api.common.karmafile.karmayaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.api.common.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
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

    private final static Set<PluginModule> loaded = new HashSet<>();
    private final static Map<Class<? extends PluginModule>, PluginModule> clazz_map = new ConcurrentHashMap<>();

    private final static File modulesFolder = new File(FileUtilities.getProjectFolder() + File.separator + "LockLogin" + File.separator + "plugin", "modules");

    /**
     * Check if the module is loaded
     *
     * @param module the module
     * @return if the module is loaded
     */
    public static boolean isLoaded(final @Nullable PluginModule module) {
        if (module != null) {
            for (PluginModule mod : loaded) {
                if (mod.name().equalsIgnoreCase(module.name()))
                    return true;
            }
        }

        return false;
    }

    /**
     * Get a module file by its module name
     *
     * @param name the module name
     * @return the module file
     */
    @Nullable
    public static File getModuleFile(final String name) {
        try {
            File moduleFile = new File(modulesFolder, name);
            if (moduleFile.exists() && moduleFile.isFile() && moduleFile.getName().endsWith(".jar")) {
                return moduleFile;
            } else {
                moduleFile = new File(modulesFolder, name + ".jar");
                if (moduleFile.exists()) {
                    return moduleFile;
                } else {
                    File[] files = modulesFolder.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && file.getName().endsWith(".jar")) {
                                JarFile jar = new JarFile(file);
                                ZipEntry entry = jar.getEntry("module.yml");
                                if (entry != null) {
                                    InputStream stream = jar.getInputStream(entry);
                                    if (stream != null) {
                                        KarmaYamlManager manager = new KarmaYamlManager(stream);
                                        String module_name = manager.getString("name", "");
                                        if (name.equalsIgnoreCase(module_name)) {
                                            return moduleFile;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Get a plugin module by name
     *
     * @param moduleFile the plugin module file
     * @return the plugin module
     */
    @Nullable
    public static PluginModule getByFile(final @NotNull File moduleFile) {
        PluginModule loadedModule = null;

        for (PluginModule module : loaded) {
            if (module.getModule().equals(moduleFile)) {
                loadedModule = module;
                break;
            }
        }

        if (loadedModule == null) {
            String extension = FileUtilities.getExtension(moduleFile);
            String name = StringUtils.replaceLast(moduleFile.getName(), "." + extension, "");

            try {
                JarFile jar = new JarFile(moduleFile);
                ZipEntry entry = jar.getEntry("module.yml");
                if (entry != null) {
                    InputStream module_yml = jar.getInputStream(entry);
                    if (module_yml != null) {
                        Yaml yaml = new Yaml();
                        Map<String, Object> values = yaml.load(module_yml);

                        String module_class_name = values.getOrDefault("loader_" + CurrentPlatform.getPlatform().name().toLowerCase(), "").toString();
                        if (!StringUtils.isNullOrEmpty(module_class_name)) {
                            JarAppender appender = CurrentPlatform.getPluginAppender();
                            appender.addJarToClasspath(moduleFile);

                            Class<? extends PluginModule> module_class;
                            if (appender.getLoader() != null) {
                                module_class = appender.getLoader().loadClass(module_class_name).asSubclass(PluginModule.class);
                            } else {
                                URLClassLoader loader = new URLClassLoader(
                                        new URL[]{new URL("file:///" + moduleFile.getAbsolutePath().replaceAll("%20", " "))}, CurrentPlatform.getMain().getClassLoader());

                                module_class = loader.loadClass(module_class_name).asSubclass(PluginModule.class);
                            }

                            loadedModule = module_class.getDeclaredConstructor().newInstance();
                        } else {
                            Console.send("&cModule {0} failed to load ( loader class not found for {1} )", name, CurrentPlatform.getPlatform().name().toLowerCase());
                        }

                        module_yml.close();
                    }

                    jar.close();
                } else {
                    Console.send("&cModule {0} failed to load ( invalid or null module.yml )", name);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        return loadedModule;
    }

    /**
     * Get a module by its module class
     *
     * @param clazz the module class
     * @return the plugin module attached
     * to that module clazz
     */
    @SuppressWarnings("unused")
    public static @Nullable PluginModule getProvidingModule(final Class<? extends PluginModule> clazz) {
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
     * @param moduleFile the module file
     * @param rule the rule the module must have
     *             to be loaded
     */
    public final synchronized void loadModule(final File moduleFile, final LoadRule rule) {
        if (moduleFile.isFile()) {
            String extension = FileUtilities.getExtension(moduleFile);

            if (extension.equals("jar")) {
                String name = StringUtils.replaceLast(moduleFile.getName(), "." + extension, "");

                PluginModule loadedModule = null;
                for (PluginModule module : loaded) {
                    if (module.getModule().equals(moduleFile)) {
                        loadedModule = module;
                        break;
                    }
                }

                if (loadedModule != null) {
                    if (loadedModule.loadRule().equals(rule)) {
                        Console.send("&cModule {0} failed to load ( already loaded )", name);
                    }
                } else {
                    try {
                        JarFile jar = new JarFile(moduleFile);
                        ZipEntry entry = jar.getEntry("module.yml");
                        if (entry != null) {
                            InputStream module_yml = jar.getInputStream(entry);
                            if (module_yml != null) {
                                KarmaYamlManager manager = new KarmaYamlManager(module_yml);

                                String module_class_name = manager.getString("loader_" + CurrentPlatform.getPlatform().name().toLowerCase(), "");
                                if (!StringUtils.isNullOrEmpty(module_class_name)) {
                                    JarAppender appender = CurrentPlatform.getPluginAppender();
                                    appender.addJarToClasspath(moduleFile);

                                    Class<? extends PluginModule> module_class;
                                    boolean pluginAppender = false;
                                    if (appender.getLoader() != null) {
                                        module_class = appender.getLoader().loadClass(module_class_name).asSubclass(PluginModule.class);
                                    } else {
                                        URLClassLoader loader = new URLClassLoader(
                                                new URL[]{new URL("file:///" + moduleFile.getAbsolutePath().replaceAll("%20", " "))}, CurrentPlatform.getMain().getClassLoader());

                                        module_class = loader.loadClass(module_class_name).asSubclass(PluginModule.class);
                                        pluginAppender = true;
                                    }

                                    PluginModule module = module_class.getDeclaredConstructor().newInstance();
                                    if (module.loadRule().equals(rule)) {
                                        loaded.add(module);

                                        if (pluginAppender) {
                                            module.setAppender(CurrentPlatform.getPluginAppender());
                                        } else {
                                            module.setAppender(module.getAppender());
                                        }

                                        module.getAppender().addJarToClasspath(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation());

                                        ModuleStatusChangeEvent event = new ModuleStatusChangeEvent(ModuleStatusChangeEvent.Status.LOAD, module, this, null);
                                        ModulePlugin.callEvent(event);

                                        clazz_map.put(module_class, module);

                                        module.enable();
                                        module.getPlugin().getVersionManager().updaterEnabled();

                                        CurrentPlatform.getPluginAppender().addJarToClasspath(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation());
                                    }
                                } else {
                                    Console.send("&cModule {0} failed to load ( loader class not found for {1} )", name, CurrentPlatform.getPlatform().name().toLowerCase());
                                }

                                module_yml.close();
                            }

                            jar.close();
                        } else {
                            Console.send("&cModule {0} failed to load ( invalid or null module.yml )", name);
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Unload the specified module
     *
     * @param moduleFile the module file
     */
    public final void unloadModule(final File moduleFile) {
        PluginModule loadedModule = null;
        for (PluginModule module : loaded) {
            if (module.getModule().equals(moduleFile)) {
                loadedModule = module;
                break;
            }
        }

        if (loadedModule != null) {
            loadedModule.getPlugin().unregisterListeners();
            loadedModule.getPlugin().unregisterCommands();

            loadedModule.disable();
            loadedModule.getAppender().close();
            loaded.remove(loadedModule);
            clazz_map.remove(loadedModule.getClass());
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