package ml.karmaconfigs.locklogin.api.event.plugin;

import ml.karmaconfigs.locklogin.api.event.util.Event;
import ml.karmaconfigs.locklogin.api.modules.JavaModuleLoader;
import ml.karmaconfigs.locklogin.api.modules.bukkit.JavaModule;
import org.jetbrains.annotations.Nullable;

/**
 * This event is fired when the plugin
 * status changes, from {@link Status#LOAD} to {@link Status#UNLOAD},
 * {@link Status#RELOAD_START} to {@link Status#RELOAD_END}, or
 * {@link Status#UPDATE_START} to {@link Status#RELOAD_END}
 */
public final class ModuleStatusChangeEvent extends Event {

    private boolean handled = false;
    private final Status status;
    private final JavaModule target;
    private final JavaModuleLoader loader;

    private final Object eventObj;

    /**
     * Initialize the event
     *
     * @param _status the plugin status
     * @param module the module that has changed
     * @param currentLoader the used loader
     * @param event the event in where this event is fired
     */
    public ModuleStatusChangeEvent(final Status _status, final JavaModule module, final JavaModuleLoader currentLoader, final Object event) {
        status = _status;
        target = module;
        loader = currentLoader;
        eventObj = event;
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
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    @Override
    public final boolean isHandled() {
        return handled;
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
    public final JavaModule getModule() {
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
        return null;
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
