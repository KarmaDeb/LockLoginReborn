package ml.karmaconfigs.locklogin.api.modules.api.event.util;

import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;

/**
 * LockLogin advanced module event handler
 */
public interface EventListener {

    /**
     * Register the event listener
     *
     * @param module the module owner
     */
    default void register(final PluginModule module) {
        JavaModuleManager manager = new JavaModuleManager(module);
        manager.registerListener(this);
    }

    /**
     * Unregister the event listener
     *
     * @param module the module owner
     */
    default void unregister(final PluginModule module) {
        JavaModuleManager manager = new JavaModuleManager(module);
        manager.unregisterListener(this);
    }
}
