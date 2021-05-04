package ml.karmaconfigs.locklogin.api.modules.javamodule;

import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.modules.ModuleDependencyLoader;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;
import ml.karmaconfigs.locklogin.api.modules.event.plugin.ModuleStatusChangeEvent;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * LockLogin java module loader
 */
public final class JavaModuleLoader {

    private final static Set<PluginModule> loaded = new LinkedHashSet<>();
    private final static Set<String> load_queue = new LinkedHashSet<>();

    private static File modulesFolder = null;

    /**
     * Initialize the loader
     *
     * @param modules the modules folder
     */
    public JavaModuleLoader(final File modules) {
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

        return null;
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
                                                URLClassLoader loader = new URLClassLoader(
                                                        new URL[]{new URL("file:///" + moduleFile.getAbsolutePath())}, main.getClassLoader());

                                                Class<?> module_main = Class.forName(class_name, true, loader);
                                                Class<? extends PluginModule> module_class = module_main.asSubclass(PluginModule.class);

                                                File lockloginFile = new File(main.getProtectionDomain()
                                                        .getCodeSource()
                                                        .getLocation()
                                                        .getPath());
                                                ModuleDependencyLoader manager = new ModuleDependencyLoader(lockloginFile);
                                                ModuleDependencyLoader subManager = new ModuleDependencyLoader(moduleFile);

                                                manager.inject(module_class);
                                                subManager.inject(main);

                                                loader.close();

                                                PluginModule module = module_class.getDeclaredConstructor().newInstance();
                                                loaded.add(module);

                                                ModuleStatusChangeEvent event = new ModuleStatusChangeEvent(ModuleStatusChangeEvent.Status.LOAD, module, this, null);
                                                JavaModuleManager.callEvent(event);

                                                module.onEnable();
                                                load_queue.remove(moduleFile.getName());
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
        if (modulesFolder != null) {
            File[] files = modulesFolder.listFiles();

            if (files != null) {
                if (isLoaded(name)) {
                    PluginModule module = getByName(name);
                    if (module != null) {
                        module.getManager().unregisterListeners();
                        module.getManager().unregisterCommands();

                        module.onDisable();
                        loaded.remove(module);
                    } else {
                        throw new IllegalArgumentException("Tried to unload module " + name + " but it's not loaded!");
                    }
                }
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