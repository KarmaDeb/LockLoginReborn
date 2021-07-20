package eu.locklogin.api.module.plugin.javamodule;

/*
 * This file is part of KarmaAPI, licensed under the MIT License.
 *
 *  Copyright (c) karma (KarmaDev) <karmaconfigs@gmail.com>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

import eu.locklogin.api.module.PluginModule;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.timer.TimeCondition;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.errors.IllegalTimerAccess;
import ml.karmaconfigs.api.common.timer.scheduler.errors.TimerAlreadyStarted;
import ml.karmaconfigs.api.common.timer.scheduler.errors.TimerNotFound;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Default simple scheduler timer
 */
@SuppressWarnings("unused")
public final class ModuleScheduler extends SimpleScheduler {

    private final int original;

    private final int id;

    private final KarmaSource source;
    private final PluginModule module;

    private int back;
    private long period = TimeUnit.SECONDS.toMillis(1);

    private boolean cancel = false;
    private boolean pause = false;
    private boolean restart;
    private boolean temp_restart = false;
    private boolean thread = false;

    private final static Map<Integer, SimpleScheduler> timersData = new ConcurrentHashMap<>();
    private final static Map<KarmaSource, Set<Integer>> runningTimers = new ConcurrentHashMap<>();

    private final Map<Integer, Set<Runnable>> secondsActions = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Consumer<Integer>>> secondsConsumer = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Consumer<Long>>> secondsLongConsumer = new ConcurrentHashMap<>();

    private final Set<Runnable> onEndTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Runnable> onStartTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Runnable> onRestartTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private Consumer<Long> pauseAction = null;
    private Consumer<Long> cancelAction = null;

    /**
     * Create a new timer
     *
     * @param owner the timer owner
     * @param time the timer time
     * @param autoRestart if the timer should auto restart
     */
    public ModuleScheduler(final PluginModule owner, final Number time, final boolean autoRestart) {
        super(owner);
        module = owner;
        source = owner;
        restart = autoRestart;

        original = (int) time.longValue();
        back = original;

        id = getId();

        timersData.put(id, this);
    }

    /**
     * Get a timer from its id
     *
     * @param owner the source that is trying to access
     *              the timer
     * @param builtId the timer id
     * @throws TimerNotFound if the timer can't be
     * @throws IllegalTimerAccess if the timer is owned by other source
     */
    public ModuleScheduler(final PluginModule owner, final int builtId) throws TimerNotFound, IllegalTimerAccess {
        super(owner);
        SimpleScheduler built = timersData.getOrDefault(builtId, null);
        if (built != null) {
            if (built.getSource().isSource(owner)) {
                module = owner;
                source = built.getSource();
                restart = built.autoRestart();

                original = (int) built.getOriginalTime();
                back = (int) TimeUnit.MILLISECONDS.toSeconds(built.getMillis());

                id = builtId;
            } else {
                throw new IllegalTimerAccess(owner, built);
            }
        } else {
            throw new TimerNotFound(builtId);
        }
    }

    /**
     * Cancel the scheduler
     */
    @Override
    public final void cancel() {
        cancel = true;
    }

    /**
     * Pause the scheduler
     */
    @Override
    public final void pause() {
        pause = true;

        if (pauseAction != null)
            runSecondsLongWithThread(pauseAction);
    }

    /**
     * Start the scheduler
     *
     * @throws TimerAlreadyStarted if the timer is
     * already started
     */
    @Override
    public final void start() throws TimerAlreadyStarted {
        Set<Integer> ids = runningTimers.getOrDefault(source, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        if (!ids.contains(id)) {
            onStartTasks.forEach(ModuleScheduler.this::runTaskWithThread);

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (ModuleLoader.isLoaded(module)) {
                        if (!pause) {
                            if (cancel || temp_restart) {
                                if (!temp_restart) {
                                    timersData.remove(id);

                                    Set<Integer> ids = runningTimers.getOrDefault(source, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                                    ids.remove(id);

                                    runningTimers.put(source, ids);

                                    if (cancelAction != null)
                                        runSecondsLongWithThread(cancelAction);

                                    cancel = false;
                                    pause = false;
                                    temp_restart = false;

                                    timer.cancel();
                                } else {
                                    back = original;
                                    onRestartTasks.forEach(ModuleScheduler.this::runTaskWithThread);
                                    temp_restart = false;
                                }
                            } else {
                                executeTasks();

                                if (back >= 0) {
                                    back--;
                                } else {
                                    back = original;
                                    if (restart) {
                                        onRestartTasks.forEach(ModuleScheduler.this::runTaskWithThread);
                                        back = original;
                                    } else {
                                        onEndTasks.forEach(ModuleScheduler.this::runTaskWithThread);
                                        timersData.remove(id);

                                        Set<Integer> ids = runningTimers.getOrDefault(source, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                                        ids.remove(id);

                                        runningTimers.put(source, ids);

                                        cancel = false;
                                        pause = false;
                                        temp_restart = false;

                                        timer.cancel();
                                    }
                                }
                            }
                        }
                    } else {
                        cancel();
                    }
                }
            }, 0, period);
        } else {
            throw new TimerAlreadyStarted(this);
        }
    }

    /**
     * Restart the scheduler
     */
    @Override
    public final void restart() {
        temp_restart = true;
    }

    /**
     * Update auto restart configuration
     *
     * @param status the auto restart status
     * @return the simple scheduler instance
     */
    @Override
    public SimpleScheduler updateAutoRestart(boolean status) {
        restart = status;

        return this;
    }

    /**
     * Set the scheduler period
     *
     * @param time the scheduler period time
     * @return the simple scheduler instance
     */
    public final SimpleScheduler withPeriod(final Number time) {
        String value = time.toString();
        int seconds;
        int milli = 0;
        if (value.contains(".")) {
            String[] data = value.split("\\.");

            String first = data[0];
            String millis = value.replaceFirst(first + ".", "");

            seconds = (int) TimeUnit.SECONDS.toMillis(Integer.parseInt(first));
            if (millis.length() != 2) {
                if (millis.length() < 2) {
                    millis = millis + "000";
                }

                milli = Integer.parseInt(millis.substring(0, 2));
            } else {
                milli = Integer.parseInt(millis);
            }
        } else {
            seconds = (int) TimeUnit.SECONDS.toMillis(time.intValue());
        }

        period = seconds + milli;

        return this;
    }

    /**
     * Set if the timer runs in another thread
     *
     * @param status if the timer should run in another
     *               thread
     * @return the simple scheduler instance
     */
    @Override
    public final SimpleScheduler multiThreading(boolean status) {
        thread = status;

        return this;
    }

    /**
     * Add an action that will be run when the timer
     * reaches the specified time
     *
     * @param time the time
     * @param task the task to run
     * @return the simple scheduler instance
     */
    @Override
    public SimpleScheduler exactSecondPeriodAction(int time, Runnable task) {
        Set<Runnable> actions = secondsActions.getOrDefault(time, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        actions.add(task);

        secondsActions.put(time, actions);

        return this;
    }

    /**
     * Add an action that will be run when the timer
     * reaches the specified time
     *
     * @param time the time
     * @param task the task to run
     * @return the simple scheduler instance
     */
    @Override
    public SimpleScheduler exactPeriodAction(long time, Runnable task) {
        Set<Runnable> actions = secondsActions.getOrDefault((int) TimeUnit.MILLISECONDS.toSeconds(time), Collections.newSetFromMap(new ConcurrentHashMap<>()));
        actions.add(task);

        secondsActions.put((int) TimeUnit.MILLISECONDS.toSeconds(time), actions);

        return this;
    }

    /**
     * Add an action whenever a second changes
     *
     * @param action the action to run
     * @return the simple scheduler instance
     */
    @Override
    public final SimpleScheduler secondChangeAction(Consumer<Integer> action) {
        int second = original;
        while (second >= 0) {
            Set<Consumer<Integer>> actions = secondsConsumer.getOrDefault(second--, Collections.newSetFromMap(new ConcurrentHashMap<>()));
            actions.add(action);

            secondsConsumer.put(second, actions);
        }

        return this;
    }

    /**
     * Add an action whenever the period changes
     * the time
     *
     * @param action the action to run
     * @return the simple scheduler instance
     */
    @Override
    public final SimpleScheduler periodChangeAction(Consumer<Long> action) {
        int second = original;
        while (second >= 0) {
            Set<Consumer<Long>> actions = secondsLongConsumer.getOrDefault(second--, Collections.newSetFromMap(new ConcurrentHashMap<>()));
            actions.add(action);

            secondsLongConsumer.put(second, actions);
        }

        return this;
    }

    /**
     * Add an action when the timer gets cancelled
     *
     * @param action the action to run
     * @return the simple scheduler instance
     */
    @Override
    public final SimpleScheduler cancelAction(Consumer<Long> action) {
        cancelAction = action;

        return this;
    }

    /**
     * Add an action when the timer gets paused
     *
     * @param action the action to run
     * @return the simple scheduler instance
     */
    @Override
    public final SimpleScheduler pauseAction(Consumer<Long> action) {
        pauseAction = action;

        return this;
    }

    /**
     * Add an action when the timer starts
     *
     * @param task the task to run
     * @return the simple scheduler instance
     */
    @Override
    public final SimpleScheduler startAction(Runnable task) {
        onStartTasks.add(task);

        return this;
    }

    /**
     * Add an action when the timer ends
     *
     * @param task the task to run
     * @return the simple scheduler instance
     */
    @Override
    public final SimpleScheduler endAction(Runnable task) {
        onEndTasks.add(task);

        return this;
    }

    /**
     * Add a task when the timer gets restarted
     *
     * @param task the task to run
     * @return the simple scheduler instance
     */
    @Override
    public final SimpleScheduler restartAction(Runnable task) {
        onRestartTasks.add(task);

        return this;
    }

    /**
     * Add a conditional task for the specified second
     *
     * @param condition       the condition
     * @param condition_value the second
     * @param action          the action to run
     * @return the simple scheduler instance
     */
    @Override
    public final SimpleScheduler conditionalAction(TimeCondition condition, int condition_value, Consumer<Integer> action) {
        Set<Consumer<Integer>> actions;

        switch (condition) {
            case EQUALS:
                actions = secondsConsumer.getOrDefault(condition_value, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                actions.add(action);

                secondsConsumer.put(condition_value, actions);
                break;
            case OVER_OF:
                int c_over_val = condition_value;
                while (c_over_val <= original) {
                    actions = secondsConsumer.getOrDefault(c_over_val++, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    actions.add(action);

                    secondsConsumer.put(c_over_val, actions);
                }
                break;
            case MINUS_TO:
                int c_minus_val = condition_value;
                while (c_minus_val >= 0) {
                    actions = secondsConsumer.getOrDefault(c_minus_val--, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    actions.add(action);

                    secondsConsumer.put(c_minus_val, actions);
                }
                break;
        }

        return this;
    }

    /**
     * Add a condition task for the specified millisecond
     *
     * @param condition       the condition
     * @param condition_value the exact second as millisecond
     * @param action          the action to run
     * @return the simple scheduler instance
     */
    @Override
    public final SimpleScheduler conditionalPeriodAction(TimeCondition condition, long condition_value, Consumer<Long> action) {
        int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(condition_value);
        Set<Consumer<Long>> actions;

        switch (condition) {
            case EQUALS:
                actions = secondsLongConsumer.getOrDefault(seconds, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                actions.add(action);

                secondsLongConsumer.put(seconds, actions);
                break;
            case OVER_OF:
                int c_over_val = seconds;
                while (c_over_val <= original) {
                    actions = secondsLongConsumer.getOrDefault(c_over_val++, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    actions.add(action);

                    secondsLongConsumer.put(c_over_val, actions);
                }
                break;
            case MINUS_TO:
                int c_minus_val = seconds;
                while (c_minus_val >= 0) {
                    actions = secondsLongConsumer.getOrDefault(c_minus_val--, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    actions.add(action);

                    secondsLongConsumer.put(c_minus_val, actions);
                }
                break;
        }

        return this;
    }

    /**
     * Get if the timer has been cancelled
     *
     * @return if the timer has been cancelled
     */
    @Override
    public final boolean isCancelled() {
        return cancel;
    }

    /**
     * Get if the timer is running
     *
     * @return if the timer is running
     */
    @Override
    public final boolean isRunning() {
        Set<Integer> ids = runningTimers.getOrDefault(source, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        return ids.contains(id);
    }

    /**
     * Get if the timer is paused
     *
     * @return if the timer is paused
     */
    @Override
    public final boolean isPaused() {
        return pause;
    }

    /**
     * Get if the timer restarts automatically
     *
     * @return if the timer restarts automatically
     */
    @Override
    public boolean autoRestart() {
        return restart;
    }

    /**
     * Get if the timer runs in another thread
     *
     * @return if the timer runs in another thread
     */
    @Override
    public boolean isMultiThreading() {
        return thread;
    }

    /**
     * Get the timer original time
     *
     * @return the timer original time
     */
    @Override
    public final long getOriginalTime() {
        return original;
    }

    /**
     * Get the timer period time
     *
     * @return the timer period time
     */
    @Override
    public final long getPeriod() {
        return period;
    }

    /**
     * Get the timer time as millis
     *
     * @return the timer time as millis
     */
    @Override
    public final long getMillis() {
        return TimeUnit.SECONDS.toMillis(back);
    }

    /**
     * Execute tasks according to back
     * value
     */
    private void executeTasks() {
        Set<Consumer<Integer>> secondConsumers = secondsConsumer.getOrDefault(back, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        Set<Consumer<Long>> secondLongConsumers = secondsLongConsumer.getOrDefault(back, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        Set<Runnable> actions = secondsActions.getOrDefault(back, Collections.newSetFromMap(new ConcurrentHashMap<>()));

        for (Consumer<Integer> consumer : secondConsumers) runSecondsWithThread(consumer);
        for (Consumer<Long> consumer : secondLongConsumers) runSecondsLongWithThread(consumer);
        for (Runnable runnable : actions) runTaskWithThread(runnable);
    }

    /**
     * Run the task according to thread option
     *
     * @param task the task to run
     */
    private void runSecondsWithThread(final Consumer<Integer> task) {
        if (thread) {
            new Thread(() -> task.accept(back)).start();
        } else {
            task.accept(back);
        }
    }

    /**
     * Run the task according to thread option
     *
     * @param task the task to run
     */
    private void runSecondsLongWithThread(final Consumer<Long> task) {
        if (thread) {
            new Thread(() -> task.accept(TimeUnit.SECONDS.toMillis(back))).start();
        } else {
            task.accept(TimeUnit.SECONDS.toMillis(back));
        }
    }

    /**
     * Run the specified task according to thread option
     *
     * @param task the task to run
     */
    private void runTaskWithThread(final Runnable task) {
        if (thread) {
            new Thread(task).start();
        } else {
            task.run();
        }
    }
}
