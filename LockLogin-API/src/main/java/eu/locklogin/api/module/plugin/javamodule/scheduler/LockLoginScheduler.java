package eu.locklogin.api.module.plugin.javamodule.scheduler;

import eu.locklogin.api.module.PluginModule;

/**
 * LockLogin scheduler, for modules and LockLogin plugin
 *
 * @apiNote In development
 */
public abstract class LockLoginScheduler {

    protected final PluginModule module;

    /**
     * Initialize the LockLogin scheduler
     *
     * @param owner the scheduler owner
     */
    public LockLoginScheduler(final PluginModule owner) {
        module = owner;
    }

    /**
     * Run the task synchronous
     *
     * @param task the task to run
     * @param delay the task delay
     * @return the scheduler that will perform the action
     */
    public abstract ModuleScheduler runAsynchronous(final Runnable task, final long delay);

    /**
     * Run the task asynchronous
     *
     * @param task the task to run
     * @param delay the task delay
     * @param period the task repeat delay
     * @return the scheduler that will perform the action
     */
    public abstract ModuleScheduler runRepeatingAsynchronous(final Runnable task, final long delay, final long period);

    /**
     * Run the task synchronous
     *
     * @param task the task to run
     * @param delay the task delay
     * @return the scheduler that will perform the action
     */
    public abstract ModuleScheduler runSynchronous(final Runnable task, final long delay);

    /**
     * Run the task synchronous
     *
     * @param task the task to run
     * @param delay the task delay
     * @param period the task repeat delay
     * @return the scheduler that will perform the action
     */
    public abstract ModuleScheduler runRepeatingSynchronous(final Runnable task, final long delay, final long period);

    /**
     * Stop the scheduler
     */
    public abstract void stop();

    /**
     * Get the module owning this scheduler
     *
     * @return the scheduler module
     */
    public final PluginModule getModule() {
        return module;
    }

    /**
     * Get the scheduler id
     *
     * @return the scheduler id
     */
    public abstract int getId();
}
