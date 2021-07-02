package eu.locklogin.api.module.plugin.javamodule;

import eu.locklogin.api.module.PluginModule;
import ml.karmaconfigs.api.common.timer.AdvancedSimpleTimer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JavaModuleScheduler {

    private final PluginModule module;
    private AdvancedSimpleTimer ast = null;

    private static int id = 0;
    private final int timerID;

    private final static Map<Integer, JavaModuleScheduler> timers = new ConcurrentHashMap<>();

    /**
     * Create a new java module scheduler
     *
     * @param owner the scheduler owner
     */
    public JavaModuleScheduler(final PluginModule owner) {
        module = owner;
        timers.put(id++, this);
        timerID = id;
    }

    /**
     * Schedule a task
     *
     * @param action the task
     * @param delay the task delay
     */
    public final void schedule(final Runnable action, final Number delay) {
        if (JavaModuleLoader.isLoaded(module)) {
            ast = new AdvancedSimpleTimer(module, delay, false).setAsync(false);
            ast.addActionOnEnd(action).start();
        }
    }

    /**
     * Schedule a task
     *
     * @param action the task
     * @param time the task time
     * @param delay the task delay
     */
    public final void scheduleRepeating(final Runnable action, final Number time, final Number delay) {
        if (JavaModuleLoader.isLoaded(module)) {
            ast = new AdvancedSimpleTimer(module, time, true).setPeriod(delay).setAsync(false);
            ast.addAction(action).start();
        }
    }

    /**
     * Schedule an async task
     *
     * @param action the task
     * @param delay the task delay
     */
    public final void scheduleAsync(final Runnable action, final Number delay) {
        if (JavaModuleLoader.isLoaded(module)) {
            ast = new AdvancedSimpleTimer(module, delay, false).setAsync(true);
            ast.addActionOnEnd(action).start();
        }
    }

    /**
     * Schedule an async task
     *
     * @param action the task
     * @param time the task time
     * @param delay the task delay
     */
    public final void scheduleAsyncRepeating(final Runnable action, final Number time, final Number delay) {
        if (JavaModuleLoader.isLoaded(module)) {
            ast = new AdvancedSimpleTimer(module, time, true).setPeriod(delay).setAsync(true);
            ast.addAction(action).start();
        }
    }

    /**
     * Cancel the current scheduler
     */
    public final void cancel() {
        if (ast != null) {
            ast.setCancelled();
        }
    }

    /**
     * Cancel an own timer using its id
     *
     * @param id the timer id
     */
    public final void cancel(final int id) {
        JavaModuleScheduler scheduler = timers.getOrDefault(id, null);
        if (scheduler != null) {
            if (scheduler.getModule().equals(module)) {
                scheduler.ast.setCancelled();
            }
        }
    }

    /**
     * Cancel all the unloaded module
     * timers
     */
    public final void cancelUnloaded() {
        for (int id : timers.keySet()) {
            JavaModuleScheduler scheduler = timers.getOrDefault(id, null);
            if (scheduler != null) {
                scheduler.ast.setCancelled();
            }
            timers.remove(id);
        }
    }

    /**
     * Get the timer id
     *
     * @return the timer id
     */
    public final int getId() {
        return timerID;
    }

    /**
     * Get the module owning this scheduler
     *
     * @return the module that owns this scheduler
     */
    public final PluginModule getModule() {
        return module;
    }
}
