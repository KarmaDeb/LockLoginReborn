package eu.locklogin.api.module.plugin.javamodule.scheduler;

import eu.locklogin.api.module.PluginModule;

/**
 * LockLogin module scheduler, for modules
 *
 * @apiNote In development
 */
public abstract class ModuleScheduler {

    protected final long delay;
    protected final PluginModule module;

    /**
     * Initialize the module scheduler
     *
     * @param owner the scheduler owner
     * @param wait the scheduler delay time
     */
    public ModuleScheduler(final PluginModule owner, final long wait) {
        module = owner;
        delay = wait;
    }

    /**
     * Get the module that registered the scheduler
     *
     * @return the module that owns this scheduler
     */
    public final PluginModule getModule() {
        return module;
    }

    /**
     * Get the scheduler delay
     *
     * @return the scheduler delay
     */
    public final long delay() {
        return delay;
    }

    /**
     * Get the scheduler period
     *
     * @return the scheduler period
     */
    public abstract long period();

    /**
     * Get if the scheduler is a looped scheduler
     *
     * @return if the scheduler is a loop scheduler
     */
    public abstract boolean repeating();
}
