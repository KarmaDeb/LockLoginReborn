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

import eu.locklogin.api.encryption.CryptoFactory;
import eu.locklogin.api.encryption.Validation;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.ModuleScheduler;
import eu.locklogin.api.module.plugin.javamodule.card.APICard;
import eu.locklogin.api.module.plugin.javamodule.card.listener.event.CardConsumedEvent;
import eu.locklogin.api.module.plugin.javamodule.card.listener.event.CardPostQueueEvent;
import eu.locklogin.api.module.plugin.javamodule.card.listener.event.CardPreConsumeEvent;
import eu.locklogin.api.module.plugin.javamodule.card.listener.event.CardQueueEvent;
import eu.locklogin.api.module.plugin.javamodule.console.MessageLevel;
import eu.locklogin.api.module.plugin.javamodule.sender.ModuleConsole;
import eu.locklogin.api.module.plugin.javamodule.updater.JavaModuleVersion;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.ModuleServer;
import eu.locklogin.api.util.platform.Platform;
import ml.karmaconfigs.api.common.Logger;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.karma.loader.BruteLoader;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;
import ml.karmaconfigs.api.common.karmafile.karmayaml.FileCopy;
import ml.karmaconfigs.api.common.karmafile.karmayaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.utils.ConcurrentList;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.file.FileUtilities;
import ml.karmaconfigs.api.common.utils.string.RandomString;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * LockLogin plugin module
 */
@SuppressWarnings("unused")
public abstract class PluginModule implements KarmaSource {

    private BruteLoader appender;
    private final Map<String, List<APICard<?>>> cards = new ConcurrentHashMap<>();
    private final Map<String, APICard<?>> card = new ConcurrentHashMap<>();

    /**
     * Initialize the plugin module
     *
     * @param platform the current server platform
     * @param key the module license key
     * @throws InvalidKeyError if the license key is not valid and does not
     * match the module one
     *
     * NOTE: UNLESS YOU KNOW WHAT YOU ARE DOING, DO NOT OVERWRITE THIS
     */
    public PluginModule(final Platform platform, final String key) throws InvalidKeyError {
        if (!StringUtils.isNullOrEmpty(key)) {
            try {
                Field stored_key = getClass().getField("module_key");
                Field allowed_platforms = getClass().getField("module_platform");

                if (stored_key.getType().equals(String.class) && allowed_platforms.getType().equals(Platform[].class)) {
                    String str = (String) stored_key.get(this);
                    Platform[] platforms = (Platform[]) allowed_platforms.get(this);

                    boolean hasPlatform = false;
                    for (Platform useful : platforms) {
                        if (useful.equals(platform)) {
                            hasPlatform = true;
                            break;
                        }
                    }
                    if (hasPlatform) {
                        if (!StringUtils.isNullOrEmpty(str)) {
                            CryptoFactory factory = CryptoFactory.getBuilder()
                                    .withPassword(stored_key)
                                    .withToken(key).build();

                            if (factory.validate(Validation.MODERN)) {
                                getConsole().sendMessage(MessageLevel.INFO, "Validated module key successfully");
                            } else {
                                throw new InvalidKeyError(this);
                            }
                        } else {
                            getConsole().sendMessage(MessageLevel.ERROR, "Module has invalid internal key. Make sure it's up to date");
                        }
                    } else {
                        getConsole().sendMessage(MessageLevel.INFO, "Module is not compatible with {1}", CurrentPlatform.getPlatform().name());
                    }
                } else {
                    getConsole().sendMessage(MessageLevel.ERROR, "Module is invalid. Make sure it's up to date");
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Initialize the plugin module
     *
     * @throws IllegalAccessError if the module
     *                            is already initialized
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
    public abstract void enable();

    /**
     * On module disable logic
     */
    public abstract void disable();

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
     * @param info  the info to log
     */
    public final void log(final Level level, final Object info) {
        Logger logger = new Logger(this);

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
     * Queue an API card
     *
     * @param card the API card
     */
    public final void queueCard(final APICard<?> card) {
        List<APICard<?>> loaded = cards.getOrDefault(card.identifier(), new ConcurrentList<>());
        CardQueueEvent event = new CardQueueEvent(card.module(), this, card.identifier());
        APICard.invoke(event);

        if (!event.isCancelled()) {
            loaded.add(card);
            cards.put(card.identifier(), loaded);

            CardPostQueueEvent post = new CardPostQueueEvent(card.module(), this, card.identifier());
            APICard.invoke(post);
        }
    }

    /**
     * Consume a card identifier
     *
     * @param identifier the identifier
     * @return if the identifier has more items in the queue
     */
    public final boolean consumeCard(final String identifier) {
        List<APICard<?>> loaded = cards.getOrDefault(name() + ":" + identifier, new ConcurrentList<>());
        if (loaded.size() > 0) {
            try {
                APICard<?> tmp = loaded.get(0);

                if (tmp != null) {
                    CardPreConsumeEvent event = new CardPreConsumeEvent(tmp.module(), this, identifier);
                    APICard.invoke(event);

                    if (!event.isCancelled()) {
                        loaded.remove(0);
                        card.put(identifier, tmp);
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return loaded.size() - 1 > 0;
    }

    /**
     * Get the current card
     *
     * @param identifier the card identifier
     * @param <A> the card type
     * @return the card
     */
    @SuppressWarnings("unchecked")
    public final <A> APICard<A> getCard(final String identifier) {
        APICard<A> tmp = (APICard<A>) card.getOrDefault(name() + ":" + identifier, null);

        CardConsumedEvent event = new CardConsumedEvent(tmp.module(), this, identifier);
        APICard.invoke(event);

        return tmp;
    }

    /**
     * Consume a card identifier
     *
     * @param owner the card owner
     * @param identifier the identifier
     * @return if the identifier has more items in the queue
     */
    public final boolean consumeCard(final PluginModule owner, final String identifier) {
        List<APICard<?>> loaded = cards.getOrDefault(owner.name() + ":" + identifier, new ConcurrentList<>());
        try {
            APICard<?> tmp = loaded.get(0);

            if (tmp != null) {
                CardPreConsumeEvent event = new CardPreConsumeEvent(tmp.module(), this, identifier);
                APICard.invoke(event);

                if (!event.isCancelled()) {
                    loaded.remove(0);
                    card.put(identifier, tmp);
                }
            }
        } catch (Throwable ignored) {}

        return loaded.size() - 1 > 0;
    }

    /**
     * Get the current card
     *
     * @param owner the card owner
     * @param identifier the card identifier
     * @param <A> the card type
     * @return the card
     */
    @SuppressWarnings("unchecked")
    public final <A> APICard<A> getCard(final PluginModule owner, final String identifier) {
        APICard<A> tmp = (APICard<A>) card.getOrDefault(owner.name() + ":" + identifier, null);

        CardConsumedEvent event = new CardConsumedEvent(tmp.module(), this, identifier);
        APICard.invoke(event);

        return tmp;
    }

    /**
     * Get the KarmaSource
     *
     * @return the karma source
     */
    public final KarmaSource getSource() {
        return this;
    }

    /**
     * Get the module appender
     *
     * @return the module appender
     */
    public final BruteLoader getAppender() {
        if (appender == null)
            return new BruteLoader((URLClassLoader) getClass().getClassLoader());
        else
            return appender;
    }

    /**
     * Set the module appender, very util for velocity
     * module or if the developer has his own
     * jar appender
     *
     * @param newAppender the new appender
     */
    public final void setAppender(final BruteLoader newAppender) {
        appender = newAppender;
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
            ModuleLoader loader = new ModuleLoader();

            PluginModule module = ModuleLoader.getByFile(getModule());
            if (!ModuleLoader.isLoaded(module)) {
                loader.loadModule(getModule(), loadRule());

                return true;
            }
        } catch (Throwable ignored) {
        }

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
            FileCopy copy = new FileCopy(this.getClass(), resource);
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
            FileCopy copy = new FileCopy(this.getClass(), resource);
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

            JarFile jar = new JarFile(new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
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

            JarFile jar = new JarFile(new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
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

            JarFile jar = new JarFile(new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
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

            JarFile jar = new JarFile(new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
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

            JarFile jar = new JarFile(new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
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

            JarFile jar = new JarFile(new File(FileUtilities.getProjectFolder("plugins") + File.separator + "LockLogin" + File.separator + "plugin" + File.separator + "modules", flName));
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
        InputStream stream = null;

        try {
            ZipFile zip = new ZipFile(getModule());
            StringBuilder pathBuilder = new StringBuilder();
            for (String dir : dirs)
                pathBuilder.append(dir).append("/");

            String path = StringUtils.replaceLast(pathBuilder.toString(), "/", "");
            ZipEntry entry = zip.getEntry(path + "/" + name);
            stream = zip.getInputStream(entry);

            zip.close();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        return stream;
    }

    /**
     * Get the module data folder
     *
     * @return the module data folder
     */
    public final File getDataFolder() {
        File mainJar = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " "));
        File parent = mainJar.getParentFile();
        File dataFolder;
        if (StringUtils.isNullOrEmpty(this.name())) {
            dataFolder = new File(parent, StringUtils.generateString(RandomString.createBuilder().withSize(5)).create());
        } else {
            dataFolder = new File(parent, this.name());
        }

        return dataFolder;
    }

    /**
     * Get the module data path
     *
     * @return the module data path
     */
    @Override
    public final Path getDataPath() {
        File mainJar = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " "));
        File parent = mainJar.getParentFile();
        File dataFolder;
        if (StringUtils.isNullOrEmpty(this.name())) {
            dataFolder = new File(parent, StringUtils.generateString(RandomString.createBuilder().withSize(5)).create());
        } else {
            dataFolder = new File(parent, this.name());
        }

        return dataFolder.toPath();
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
        Path main = getDataPath();
        for (String dir : path)
            main = main.resolve(dir);

        return main.resolve(name).toFile();
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
