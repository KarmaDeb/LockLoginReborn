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

import eu.locklogin.api.module.InvalidKeyError;
import eu.locklogin.api.module.LoadRule;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.Platform;
import ml.karmaconfigs.api.common.karma.APISource;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.loader.BruteLoader;
import ml.karmaconfigs.api.common.karmafile.karmayaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.common.utils.url.HttpUtil;
import ml.karmaconfigs.api.common.utils.url.URLUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

        for (PluginModule module : loaded.values()) {
            if (module.getModule().equals(moduleFile)) {
                loadedModule = module;
                break;
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
                    JarFile jar = new JarFile(moduleFile);
                    JarEntry plugin = jar.getJarEntry("module.yml");

                    if (plugin != null) {
                        Yaml yaml = new Yaml();
                        Map<String, Object> values = yaml.load(jar.getInputStream(plugin));
                        name = values.getOrDefault("name", FileUtilities.getName(moduleFile, false)).toString();
                        String class_name = values.getOrDefault("loader_" + CurrentPlatform.getPlatform().name().toLowerCase(), null).toString();String load_rule = values.getOrDefault("load", "PREPLUGIN").toString();
                        LoadRule lr = LoadRule.PREPLUGIN;
                        if (load_rule.equalsIgnoreCase("POSTPLUGIN"))
                            lr = LoadRule.POSTPLUGIN;

                        if (lr.equals(rule)) {
                            if (class_name == null && ((boolean) values.getOrDefault("obfuscated", false))) {
                                class_name = values.getOrDefault("license", "").toString();
                                source.console().send("Validating obfuscated resource load license for security reasons. ONLY TRUSTED OBFUSCATED MODULES SHOULD BE LOADED!. Remember to have your module up to date for this to work!", Level.WARNING);

                                String[] check = new String[]{
                                        "https://karmaconfigs.ml",
                                        "https://karmarepo.ml",
                                        "https://karmadev.es",
                                        "https://backup.karmaconfigs.ml",
                                        "https://backup.karmarepo.ml",
                                        "https://backup.karmadev.es"
                                };

                                for (String str : check) {
                                    if (URLUtils.exists(str)) {
                                        String complete_url = str + "/locklogin/api/?license=" + class_name;
                                        URL url = URLUtils.getOrNull(complete_url);
                                        if (url != null) {
                                            HttpUtil util = URLUtils.extraUtils(url);
                                            if (util != null) {
                                                String response = util.getResponse();
                                                if (!StringUtils.isNullOrEmpty(response)) {
                                                    Yaml json = new Yaml();
                                                    Map<String, Object> data = json.load(response);

                                                    boolean validation = (boolean) data.getOrDefault("status", false);
                                                    String uniqueKey = data.getOrDefault("key", "").toString();

                                                    BruteLoader appender = CurrentPlatform.getPluginAppender();
                                                    appender.add(moduleFile);
                                                    try (JarFile jarFile = new JarFile(moduleFile)) {
                                                        Enumeration<JarEntry> e = jarFile.entries();
                                                        while (e.hasMoreElements()) {
                                                            JarEntry jarEntry = e.nextElement();
                                                            if (jarEntry.getName().endsWith(".class")) {
                                                                String className = jarEntry.getName()
                                                                        .replace("/", ".")
                                                                        .replace(".class", "");

                                                                try {
                                                                    Class<?> clazz = appender.getLoader().loadClass(className);
                                                                    if (clazz.isAssignableFrom(PluginModule.class) || PluginModule.class.isAssignableFrom(clazz)) {
                                                                        Class<? extends PluginModule> moduleClass = clazz.asSubclass(PluginModule.class);
                                                                        try {
                                                                            Constructor<? extends PluginModule> expectedConstructor = moduleClass.getConstructor(Platform.class, String.class);
                                                                            expectedConstructor.setAccessible(true);

                                                                            try {
                                                                                PluginModule module = expectedConstructor.newInstance(CurrentPlatform.getPlatform(), uniqueKey);
                                                                                if (!isLoaded(module)) {
                                                                                    loaded.put(FileUtilities.getPrettyFile(moduleFile), module);

                                                                                    module.enable();
                                                                                }
                                                                            } catch (InvalidKeyError error) {
                                                                                source.console().send("Failed to load module {0} ( invalid key provided )", Level.GRAVE, name);
                                                                            }
                                                                        } catch (Throwable ignored) {}
                                                                    }
                                                                } catch (Throwable ignored) {}
                                                            }
                                                        }
                                                    }

                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (class_name != null) {
                                    BruteLoader appender = CurrentPlatform.getPluginAppender();
                                    appender.add(moduleFile);

                                    Class<? extends PluginModule> module_class = appender.getLoader().loadClass(class_name).asSubclass(PluginModule.class);

                                    PluginModule module = null;
                                    try {
                                        module = module_class.getDeclaredConstructor().newInstance();
                                    } catch (InvocationTargetException ignored) {
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
     * Unload the specified module
     *
     * @param moduleFile the module file
     */
    public void unloadModule(final File moduleFile) {
        PluginModule loadedModule = loaded.getOrDefault(FileUtilities.getPrettyFile(moduleFile), null);

        if (loadedModule != null) {
            loadedModule.getPlugin().unregisterListeners();
            loadedModule.getPlugin().unregisterCommands();

            loadedModule.disable();
            loaded.remove(FileUtilities.getPrettyFile(moduleFile));
            clazz_map.remove(loadedModule.getClass());
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