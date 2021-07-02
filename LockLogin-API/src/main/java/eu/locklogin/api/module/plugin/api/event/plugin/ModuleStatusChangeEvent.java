package eu.locklogin.api.module.plugin.api.event.plugin;

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

import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.JavaModuleLoader;
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
    private String handleReason = "";

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
     * Get if the event is handleable or not
     *
     * @return if the event is handleable
     */
    @Override
    public boolean isHandleable() {
        return false;
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
     * Get the reason of why the event has been
     * marked as handled
     *
     * @return the event handle reason
     */
    @Override
    public String getHandleReason() {
        return handleReason;
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     * @param reason the handle reason
     */
    public final void setHandled(final boolean status, final String reason) {
        handled = status;
        handleReason = reason;
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
