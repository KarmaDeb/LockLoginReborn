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
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.data.file.FileUtilities;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karma.loader.BruteLoader;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * LockLogin java module loader
 */
public final class ModuleLoader {

    private final static KarmaSource source = APISource.loadProvider("LockLogin");

    private final static Map<String, PluginModule> loaded = new ConcurrentHashMap<>();
    private final static Map<PluginModule, List<String>> module_classes = new ConcurrentHashMap<>();
    private final static Map<Class<? extends PluginModule>, PluginModule> clazz_map = new ConcurrentHashMap<>();

    private final static File modulesFolder = new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "plugin", "modules");

    /**
     * Check if the module is loaded
     *
     * @param module the module
     * @return if the module is loaded
     */
    public static boolean isLoaded(final @Nullable PluginModule module) {
        if (module != null) {
            for (PluginModule mod : loaded.values()) {
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
        for (PluginModule module : loaded.values()) {
            if (module.name().equalsIgnoreCase(name))
                return module.getModule();
        }

        try {
            File moduleFile = new File(modulesFolder, name);
            if (moduleFile.exists() && moduleFile.isFile() && moduleFile.getName().endsWith(".jar")) {
                return moduleFile;
            } else {
                File[] files = modulesFolder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        moduleFile = file;
                        if (file.isFile() && file.getName().endsWith(".jar")) {
                            try(JarFile jar = new JarFile(file)) {
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

            moduleFile = new File(modulesFolder, name + ".jar");
            if (moduleFile.exists() && moduleFile.isFile()) {
                return moduleFile;
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
    public static PluginModule getByFile(final @Nullable File moduleFile) {
        PluginModule loadedModule = null;

        if (moduleFile != null) {
            for (PluginModule module : loaded.values()) {
                if (FileUtilities.getPrettyFile(module.getModule()).equalsIgnoreCase(FileUtilities.getPrettyFile(moduleFile))) {
                    loadedModule = module;
                    break;
                }
            }
        }

        return loadedModule;
    }

    /**
     * Get a module by its unique identifier
     *
     * @param id the module ID
     * @return the module
     */
    public static PluginModule getById(final UUID id) {
        for (PluginModule module : loaded.values()) {
            if (module.getID().equals(id)) {
                return module;
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
    @SuppressWarnings("unused")
    public static @Nullable PluginModule getProvidingModule(final Class<? extends PluginModule> clazz) {
        return clazz_map.getOrDefault(clazz, null);
    }

    /**
     * Get a list of the loaded modules
     *
     * @return a set of the loaded modules
     */
    public static Set<PluginModule> getModules() {
        return new HashSet<>(loaded.values());
    }

    /**
     * Load the specified module
     *
     * @param moduleFile the module file
     * @param rule       the rule the module must have
     *                   to be loaded
     */
    public synchronized void loadModule(final File moduleFile, final LoadRule rule) {
        if (!loaded.containsKey(FileUtilities.getPrettyFile(moduleFile)) || loaded.getOrDefault(FileUtilities.getPrettyFile(moduleFile), null) == null) {
            String name = FileUtilities.getName(moduleFile, false);

            try {
                if (moduleFile.getName().endsWith(".jar")) {
                    try(JarFile jar = new JarFile(moduleFile)) {
                        JarEntry plugin = jar.getJarEntry("module.yml");

                        if (plugin != null) {
                            Yaml yaml = new Yaml();
                            Map<String, Object> values = yaml.load(jar.getInputStream(plugin));
                            name = values.getOrDefault("name", FileUtilities.getName(moduleFile, false)).toString();
                            String class_name = values.getOrDefault("loader_" + CurrentPlatform.getPlatform().name().toLowerCase(), null).toString();
                            String load_rule = values.getOrDefault("load", "PREPLUGIN").toString();
                            LoadRule lr = LoadRule.PREPLUGIN;
                            if (load_rule.equalsIgnoreCase("POSTPLUGIN"))
                                lr = LoadRule.POSTPLUGIN;

                            if (lr.equals(rule)) {
                                if (class_name != null) {
                                    BruteLoader appender = CurrentPlatform.getPluginAppender();
                                    appender.add(moduleFile);

                                    Class<? extends PluginModule> module_class = appender.getLoader().loadClass(class_name).asSubclass(PluginModule.class);

                                    PluginModule module = null;
                                    Enumeration<JarEntry> entries = jar.entries();

                                    try {
                                        module = module_class.getDeclaredConstructor().newInstance();
                                        source.console().send("Scanning module {0}. Please wait", Level.INFO, name);
                                        int found = 0;
                                        while (entries.hasMoreElements()) {
                                            JarEntry entry = entries.nextElement();
                                            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                                                String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                                                List<String> moduleClasses = module_classes.getOrDefault(module, new ArrayList<>());
                                                moduleClasses.add(className);

                                                module_classes.put(module, moduleClasses);
                                                found++;
                                            }
                                        }
                                        source.console().send("Finished scanning of {0}, found {1} classes", Level.INFO, name, found);

                                        source.logger().scheduleLog(Level.INFO, "Loaded module {0} from plugin module loader", name);
                                    } catch (InvocationTargetException ex) {
                                        source.logger().scheduleLog(Level.GRAVE, ex);
                                        source.logger().scheduleLog(Level.INFO, "Failed to load module {0}", name);

                                        source.console().send("Failed to load module {0}. More info has been stored in plugins/LockLogin/logs/", Level.GRAVE, name);
                                    }
                                    if (module != null && !isLoaded(module)) {
                                        module.getAppender().add(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation());

                                        loaded.put(FileUtilities.getPrettyFile(moduleFile), module);
                                        module.enable();
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ex) {
                source.logger().scheduleLog(Level.GRAVE, ex);
                source.logger().scheduleLog(Level.INFO, "Failed to load module {0}", name);

                source.console().send("Failed to load module {0}. More info has been stored in plugins/LockLogin/logs/", Level.GRAVE, name);
            }
        }
    }

    /**
     * Get the specified module
     *
     * @param moduleFile the module file
     * @return the module from the file
     */
    public PluginModule getModule(final File moduleFile) {
        if (!loaded.containsKey(FileUtilities.getPrettyFile(moduleFile)) || loaded.getOrDefault(FileUtilities.getPrettyFile(moduleFile), null) == null) {
            String name = FileUtilities.getName(moduleFile, false);

            try {
                if (moduleFile.getName().toLowerCase().endsWith(".jar")) {
                    try (JarFile jar = new JarFile(moduleFile)) {
                        JarEntry plugin = jar.getJarEntry("module.yml");

                        if (plugin != null) {
                            Yaml yaml = new Yaml();
                            Map<String, Object> values = yaml.load(jar.getInputStream(plugin));
                            name = values.getOrDefault("name", FileUtilities.getName(moduleFile, false)).toString();
                            String class_name = values.getOrDefault("loader_" + CurrentPlatform.getPlatform().name().toLowerCase(), null).toString();

                            if (class_name != null) {
                                BruteLoader appender = CurrentPlatform.getPluginAppender();
                                appender.add(moduleFile);

                                Class<? extends PluginModule> module_class = appender.getLoader().loadClass(class_name).asSubclass(PluginModule.class);

                                PluginModule module = null;
                                try {
                                    module = module_class.getDeclaredConstructor().newInstance();
                                } catch (InvocationTargetException ex) {
                                    ex.printStackTrace();
                                }

                                if (module != null) {
                                    module.getAppender().add(CurrentPlatform.getMain().getProtectionDomain().getCodeSource().getLocation());
                                    return module;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ex) {
                source.logger().scheduleLog(Level.GRAVE, ex);
                source.logger().scheduleLog(Level.INFO, "Failed to load module {0}", name);

                source.console().send("Failed to load module {0}. More info has been stored in plugins/LockLogin/logs/", Level.GRAVE, name);
            }
        } else {
            return getByFile(moduleFile);
        }

        return null;
    }

    /**
     * Unload the specified module
     *
     * @param moduleFile the module file
     */
    @SuppressWarnings("unchecked")
    public void unloadModule(final File moduleFile) {
        PluginModule loadedModule = loaded.getOrDefault(FileUtilities.getPrettyFile(moduleFile), null);

        if (loadedModule != null) {
            loadedModule.getPlugin().unregisterListeners();
            loadedModule.getPlugin().unregisterCommands();

            loadedModule.disable();
            loaded.remove(FileUtilities.getPrettyFile(moduleFile));
            clazz_map.remove(loadedModule.getClass());

            ClassLoader cl = source.getClass().getClassLoader();
            try {
                Field classes = cl.getClass().getField("classes");
                Vector<Class<?>> vector = (Vector<Class<?>>) classes.get(cl);

                List<Class<?>> remove = new ArrayList<>();
                for (String clazz : module_classes.getOrDefault(loadedModule, new ArrayList<>())) {
                    for (Class<?> loader : vector) {
                        if (loader.getName().equals(clazz) || loader.getCanonicalName().equals(clazz)) {
                            remove.add(loader);
                        }
                    }
                }

                source.console().send("Unloaded module {0} classes ({1})", Level.INFO, loadedModule.name(), remove.size());
                vector.removeAll(remove);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Get the modules folder
     *
     * @return the modules folder
     */
    public File getDataFolder() {
        return modulesFolder;
    }
}