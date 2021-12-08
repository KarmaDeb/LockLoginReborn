package eu.locklogin.plugin.velocity.util.scheduler;

import com.velocitypowered.api.scheduler.Scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Bukkit scheduler data
 */
final class SchedulerData {

    private final Map<Integer, Runnable> tasks = new ConcurrentHashMap<>();
    Consumer<Integer> taskStart = null;
    Consumer<Integer> taskEnd = null;
    private Scheduler scheduler;
    private int current_id;
    private int last_task_id = 0;

    /**
     * Update the scheduler instance
     *
     * @param s the scheduler
     */
    public void updateScheduler(final Scheduler s) {
        scheduler = s;
    }

    /**
     * Update the current id
     *
     * @param id the current id
     */
    public void updateId(final int id) {
        current_id = id;
    }

    /**
     * Add a new task
     *
     * @param tsk the task to add
     * @return the task id
     */
    public int addTask(final Runnable tsk) {
        int taskId = ++last_task_id;

        tasks.put(taskId, tsk);

        return taskId;
    }

    /**
     * Get the scheduler
     *
     * @return the scheduler
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Get the current task id
     *
     * @return the current task id
     */
    public int getCurrentId() {
        return current_id;
    }

    /**
     * Get a task
     *
     * @param id the task id
     * @return the task
     */
    public Runnable getTask(final int id) {
        if (tasks.containsKey(id)) {
            return tasks.remove(id);
        } else {
            return null;
        }
    }

    /**
     * Get if the scheduler has more
     * tasks
     *
     * @return if the scheduler has more
     * tasks
     */
    public boolean hasNext() {
        return tasks.containsKey(current_id + 1);
    }

    /**
     * Get the on task start listener
     *
     * @return the on task start listener
     */
    public Consumer<Integer> onTaskStart() {
        return taskStart;
    }

    /**
     * Get the on task end listener
     *
     * @return the on task end listener
     */
    public Consumer<Integer> onTaskEnd() {
        return taskEnd;
    }
}
