package ml.karmaconfigs.locklogin.api.modules.velocity;

import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import ml.karmaconfigs.api.common.utils.FileUtilities;
import ml.karmaconfigs.api.velocity.timer.AdvancedPluginTimer;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;

public abstract class JavaModule extends PluginModule {

    /**
     * Get a module instance based on the
     * main class
     *
     * @param clazz the main class
     * @return a java module instance
     */
    public static JavaModule getModuleInstance(final Class<? extends JavaModule> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Get the plugin scheduler
     *
     * @param period the timer period
     * @param repeat repeat the timer on end
     * @return the custom plugin scheduler
     */
    public final AdvancedPluginTimer getScheduler(final int period, final boolean repeat) {
        PluginContainer container = new FakeContainer();

        try {
            Class<?> lockloginInterface = Class.forName("ml.karmaconfigs.locklogin.plugin.velocity.LockLogin");
            Field plugin = lockloginInterface.getDeclaredField("plugin");

            container = (PluginContainer) plugin.get(null);
        } catch (Throwable ignored) {}

        return new AdvancedPluginTimer(container, period, repeat);
    }

    /**
     * Get the plugin scheduler
     *
     * @param period the timer period
     * @return the custom plugin scheduler
     */
    public final AdvancedPluginTimer getScheduler(final int period) {
        PluginContainer container = new FakeContainer();

        try {
            Class<?> lockloginInterface = Class.forName("ml.karmaconfigs.locklogin.plugin.velocity.LockLogin");
            Field plugin = lockloginInterface.getDeclaredField("plugin");

            container = (PluginContainer) plugin.get(null);
        } catch (Throwable ignored) {}

        return new AdvancedPluginTimer(container, period);
    }
}

class FakeContainer implements PluginContainer {

    @Override
    public Optional<?> getInstance() {
        try {
            return Optional.of(Class.forName("ml.karmaconfigs.locklogin.plugin.velocity.Main"));
        } catch (Throwable ex) {
            return Optional.empty();
        }
    }

    @Override
    public PluginDescription getDescription() {

        return new PluginDescription() {
            /**
             * Gets the name of the {@link Plugin} within this container.
             *
             * @return an {@link Optional} with the plugin name, may be empty
             * @see Plugin#name()
             */
            @Override
            public Optional<String> getName() {
                return Optional.of("LockLogin");
            }

            /**
             * Gets the version of the {@link Plugin} within this container.
             *
             * @return an {@link Optional} with the plugin version, may be empty
             * @see Plugin#version()
             */
            @Override
            public Optional<String> getVersion() {
                return Optional.of("1.0.0");
            }

            /**
             * Gets the description of the {@link Plugin} within this container.
             *
             * @return an {@link Optional} with the plugin description, may be empty
             * @see Plugin#description()
             */
            @Override
            public Optional<String> getDescription() {
                return Optional.of("Error while getting LockLogin plugin container");
            }

            /**
             * Gets the url or website of the {@link Plugin} within this container.
             *
             * @return an {@link Optional} with the plugin url, may be empty
             * @see Plugin#url()
             */
            @Override
            public Optional<String> getUrl() {
                return Optional.of("https://karmaconfigs.ml/");
            }

            /**
             * Gets the authors of the {@link Plugin} within this container.
             *
             * @return the plugin authors, may be empty
             * @see Plugin#authors()
             */
            @Override
            public List<String> getAuthors() {
                return Collections.singletonList("KarmaDev");
            }

            /**
             * Gets a {@link Collection} of all dependencies of the {@link Plugin} within this container.
             *
             * @return the plugin dependencies, can be empty
             * @see Plugin#dependencies()
             */
            @Override
            public Collection<PluginDependency> getDependencies() {
                return Collections.emptyList();
            }

            @Override
            public Optional<PluginDependency> getDependency(String id) {
                return Optional.empty();
            }

            /**
             * Returns the source the plugin was loaded from.
             *
             * @return the source the plugin was loaded from or {@link Optional#empty()} if unknown
             */
            @Override
            public Optional<Path> getSource() {
                return Optional.of(new File(FileUtilities.getPluginsFolder(), "LockLogin").toPath());
            }

            @Override
            public String getId() {
                return "locklogin";
            }
        };
    }
}