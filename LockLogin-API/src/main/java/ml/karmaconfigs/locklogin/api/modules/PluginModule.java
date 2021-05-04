package ml.karmaconfigs.locklogin.api.modules;

import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.locklogin.api.modules.javamodule.JavaModuleLoader;
import ml.karmaconfigs.locklogin.api.modules.javamodule.JavaModuleManager;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public abstract class PluginModule {

    /**
     * Get a module instance based on the
     * main class
     *
     * @param clazz the main class
     * @param <T>   class type
     * @return the main class module object
     */
    public static <T extends PluginModule> T getModule(final Class<T> clazz) {
        Validate.notNull(clazz, "Null class cannot be a java module instance");
        if (!PluginModule.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(clazz + " is not an instance of " + PluginModule.class);
        }

        return clazz.cast(PluginModule.class);
    }

    /**
     * On module enable logic
     */
    public abstract void onEnable();

    /**
     * On module disable logic
     */
    public abstract void onDisable();

    /**
     * Reload the module
     */
    public final boolean reload() {
        return unload() && load();
    }

    /**
     * Unload the module
     *
     * @return if the module could be unloaded
     */
    public final boolean unload() {
        try {
            File modulesFolder = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "plugin", "modules");

            JavaModuleLoader loader = new JavaModuleLoader(modulesFolder);

            PluginModule module = getModule(this.getClass());
            if (JavaModuleLoader.isLoaded(module.name())) {
                loader.unloadModule(module.name());

                return true;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    /**
     * Load the module
     *
     * @return if the module could be loaded
     */
    public final boolean load() {
        try {
            File modulesFolder = new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "plugin", "modules");

            JavaModuleLoader loader = new JavaModuleLoader(modulesFolder);

            PluginModule module = getModule(this.getClass());
            if (!JavaModuleLoader.isLoaded(module.name())) {
                loader.loadModule(module.name());

                return true;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    /**
     * Get the module name
     *
     * @return the module name
     */
    public final @NotNull String name() {
        String name = "name not set in module.yml";

        try {
            String flName = new File(this.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()).getName();

            JarFile jar = new JarFile(new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
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
    public final @NotNull String version() {
        String version = "version not set in module.yml";

        try {
            String flName = new java.io.File(this.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()).getName();

            JarFile jar = new JarFile(new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
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
    public @NotNull String description() {
        String description = "LockLogin module ( " + this.getClass().getPackage().getName() + " )";

        try {
            String flName = new java.io.File(this.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()).getName();

            JarFile jar = new JarFile(new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
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
     * Get the module auth
     *
     * @return the module author
     */
    public @NotNull String author() {
        String author = "KarmaDev";

        try {
            String flName = new java.io.File(this.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()).getName();

            JarFile jar = new JarFile(new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
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
                            descBuilder.append(obj).append(" - ");
                        }

                        author = StringUtils.replaceLast(descBuilder.toString(), " - ", "");
                    } else {
                        if (desc.getClass().isArray()) {
                            Object[] array = (Object[]) desc;

                            StringBuilder descBuilder = new StringBuilder();

                            for (Object obj : array) {
                                descBuilder.append(obj).append(" - ");
                            }

                            author = StringUtils.replaceLast(descBuilder.toString(), " - ", "");
                        } else {
                            author = desc.toString();
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
     * Get module update url
     *
     * @return the module update url
     */
    public @NotNull String update_url() {
        String url = "";

        try {
            String flName = new java.io.File(this.getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()).getName();

            JarFile jar = new JarFile(new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
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
        return new File(FileUtilities.getPluginsFolder() + File.separator + "LockLogin" + File.separator + File.separator + "plugin" + File.separator + "modules", this.name());
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
    public final JavaModuleManager getManager() {
        return new JavaModuleManager(this);
    }
}
