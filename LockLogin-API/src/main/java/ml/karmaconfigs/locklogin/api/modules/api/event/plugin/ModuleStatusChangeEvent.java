package ml.karmaconfigs.locklogin.api.modules.api.event.plugin;

import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleLoader;
import ml.karmaconfigs.locklogin.api.modules.PluginModule;
import ml.karmaconfigs.locklogin.api.modules.api.event.util.Event;
import org.jetbrains.annotations.Nullable;

/**
 * This event is fired when the plugin
 * status changes, from {@link Status#LOAD} or {@link Status#UNLOAD}
 */
public final class ModuleStatusChangeEvent extends Event {

    private final Status status;
    private final PluginModule target;
    private final JavaModuleLoader loader;
    private final Object eventObj;
    private boolean handled = false;

    /**
     * Initialize the event
     *
     * @param _status       the plugin status
     * @param module        the module that has changed
     * @param currentLoader the used loader
     * @param event         the event in where this event is fired
     */
    public ModuleStatusChangeEvent(final Status _status, final PluginModule module, final JavaModuleLoader currentLoader, final Object event) {
        status = _status;
        target = module;
        loader = currentLoader;
        eventObj = event;
    }

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    @Override
    public final boolean isHandled() {
        return handled;
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     */
    @Override
    public final void setHandled(boolean status) {
        handled = status;
    }

    /**
     * Get the plugin status
     *
     * @return the plugin status
     */
    public final Status getStatus() {
        return status;
    }

    /**
     * Get the module who changes
     *
     * @return the module that changed
     */
    public final PluginModule getModule() {
        return target;
    }

    /**
     * Get the loader that performed this action
     *
     * @return the loader that performed this action
     */
    public final JavaModuleLoader getLoader() {
        return loader;
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public final @Nullable Object getEvent() {
        return eventObj;
    }

    /**
     * Available plugin status
     */
    public enum Status {
        /**
         * Plugin loading status
         */
        LOAD,

        /**
         * Plugin unloading status
         */
        UNLOAD;
    }
}
