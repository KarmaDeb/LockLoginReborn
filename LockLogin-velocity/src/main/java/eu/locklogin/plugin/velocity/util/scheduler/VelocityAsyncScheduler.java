package eu.locklogin.plugin.velocity.util.scheduler;

import ml.karmaconfigs.api.common.timer.scheduler.Scheduler;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static eu.locklogin.plugin.velocity.LockLogin.plugin;
import static eu.locklogin.plugin.velocity.LockLogin.server;

/**
 * Velocity special asynchronous scheduler
 */
public class VelocityAsyncScheduler extends Scheduler {

    private static final SchedulerData data = new SchedulerData();

    /**
     * Initialize the scheduler
     */
    public VelocityAsyncScheduler() {
        if (data.getScheduler() == null) {
            com.velocitypowered.api.scheduler.Scheduler scheduler = server.getScheduler();
            scheduler.buildTask(plugin, () -> new Thread(() -> {
                int next = data.getCurrentId() + 1;

                Runnable runnable = data.getTask(next);
                if (runnable != null) {
                    if (data.onTaskStart() != null)
                        data.onTaskStart().accept(next);

                    runnable.run();
                    data.updateId(next);

                    if (data.onTaskEnd() != null)
                        data.onTaskEnd().accept(next);
                }
            })).repeat(1, TimeUnit.SECONDS).schedule();
        }
    }

    /**
     * Action to perform when a task has been
     * started
     *
     * @param taskId the action to perform
     */
    @Override
    public final void onTaskStart(final Consumer<Integer> taskId) {
        data.taskStart = taskId;
    }

    /**
     * Action to perform when a task has been
     * completed
     *
     * @param taskId the action to perform
     */
    @Override
    public final void onTaskComplete(final Consumer<Integer> taskId) {
        data.taskEnd = taskId;
    }

    /**
     * Queue another task to the scheduler
     *
     * @param task the task to perform
     * @return the task id
     */
    @Override
    public final int queue(final Runnable task) {
        return data.addTask(task);
    }

    /**
     * Get the current task id
     *
     * @return the current task id
     */
    @Override
    public final int currentTask() {
        return data.getCurrentId();
    }

    /**
     * Get if the scheduler has more tasks
     *
     * @return if the scheduler has more tasks
     */
    public final boolean hasMoreTasks() {
        return data.hasNext();
    }
}

