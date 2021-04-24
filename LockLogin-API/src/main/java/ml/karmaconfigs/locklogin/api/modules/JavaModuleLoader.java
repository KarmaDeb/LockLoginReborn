package ml.karmaconfigs.locklogin.api.modules;

import ml.karmaconfigs.locklogin.api.LockLoginListener;
import ml.karmaconfigs.locklogin.api.event.plugin.ModuleStatusChangeEvent;
import ml.karmaconfigs.locklogin.api.modules.bukkit.JavaModule;
import ml.karmaconfigs.locklogin.plugin.common.JarManager;
import ml.karmaconfigs.locklogin.plugin.common.utils.platform.CurrentPlatform;
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

    private final static Set<File> loaded = new LinkedHashSet<>();

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
     * Load the specified module
     *
     * @param name the module name
     */
    public final void loadModule(final String name) {
        if (modulesFolder != null) {
            File[] files = modulesFolder.listFiles();

            if (files != null) {
                for (File moduleFile : files) {
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
                                    if (!loaded.contains(moduleFile)) {
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
                                            Class<? extends JavaModule> module_class = module_main.asSubclass(JavaModule.class);

                                            File lockloginFile = new File(main.getProtectionDomain()
                                                    .getCodeSource()
                                                    .getLocation()
                                                    .getPath());
                                            JarManager manager = new JarManager(lockloginFile);
                                            JarManager subManager = new JarManager(moduleFile);

                                            manager.inject(module_class);
                                            subManager.inject(main);

                                            loader.close();

                                            JavaModule module = module_class.getDeclaredConstructor().newInstance();

                                            ModuleStatusChangeEvent event = new ModuleStatusChangeEvent(ModuleStatusChangeEvent.Status.LOAD, module, this, null);
                                            LockLoginListener.callEvent(event);

                                            module.onEnable();

                                            loaded.add(moduleFile);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    /**
     * Unload the specified module
     *
     * @param name the module name
     */
    public final void unloadModule(final String name) {
        if (modulesFolder != null) {
            File[] files = modulesFolder.listFiles();

            if (files != null) {
                for (File moduleFile : files) {
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
                                    if (loaded.contains(moduleFile)) {
                                        String class_name = null;

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
                                                    new URL[]{new URL("file:///" + moduleFile.getAbsolutePath())}, CurrentPlatform.getMain().getClassLoader());

                                            Class<?> module_main = Class.forName(class_name, true, loader);
                                            Class<? extends JavaModule> module_class = module_main.asSubclass(JavaModule.class);

                                            JavaModule module = module_class.getDeclaredConstructor().newInstance();
                                            module.onDisable();

                                            LockLoginListener.unregisterListeners(module);

                                            ModuleStatusChangeEvent event = new ModuleStatusChangeEvent(ModuleStatusChangeEvent.Status.UNLOAD, module, this, null);
                                            LockLoginListener.callEvent(event);

                                            loaded.remove(moduleFile);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    /**
     * Check if the module is loaded
     *
     * @param name the module name
     * @return if the module is loaded
     */
    public final boolean isLoaded(final String name) {
        for (File moduleFile : loaded) {
            try {
                JarFile jar = new JarFile(moduleFile);

                ZipEntry module_yml = jar.getEntry("module.yml");
                if (module_yml != null) {
                    InputStream module_stream = jar.getInputStream(module_yml);
                    if (module_stream != null) {
                        Yaml yaml = new Yaml();
                        Map<String, Object> values = yaml.load(module_stream);

                        String module_name = values.getOrDefault("name", "").toString();
                        if (module_name.equalsIgnoreCase(name)) {
                            return loaded.contains(moduleFile);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        return false;
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
