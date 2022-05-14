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
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.TimeCondition;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.errors.IllegalTimerAccess;
import ml.karmaconfigs.api.common.timer.scheduler.errors.TimerAlreadyStarted;
import ml.karmaconfigs.api.common.timer.scheduler.errors.TimerNotFound;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Default simple scheduler timer
 */
@SuppressWarnings("unused")
public final class ModuleScheduler extends SimpleScheduler {

    private static final Map<Integer, SimpleScheduler> timersData = new ConcurrentHashMap<>();
    /**
     * A map containing source => schedulers ID
     */
    private static final Map<KarmaSource, Set<Integer>> runningTimers = new ConcurrentHashMap<>();

    /**
     * The scheduler ID
     */
    private final int id;

    /**
     * The scheduler source
     */
    private final PluginModule module;

    /**
     * A map containing second => actions to perform
     */
    private final Map<Long, Set<Runnable>> periodActions;
    /**
     * A map containing second => actions to perform
     */
    private final Map<Integer, Set<Runnable>> secondsActions;
    /**
     * A map containing second => actions to perform
     */
    private final Map<Long, Set<Consumer<Integer>>> secondsConsumer;
    /**
     * A map containing second => actions to perform
     */
    private final Map<Long, Set<Consumer<Long>>> periodConsumer;
    /**
     * A map containing unit => actions to perform
     */
    private final Map<SchedulerUnit, Set<Consumer<Integer>>> schedulerConsumer;

    /**
     * Actions to perform when the scheduler ends
     */
    private final Set<Runnable> onEndTasks;
    /**
     * Actions to perform when the scheduler starts
     */
    private final Set<Runnable> onStartTasks;
    /**
     * Actions to perform when the scheduler restarts
     */
    private final Set<Runnable> onRestartTasks;

    /**
     * The original scheduler time
     */
    private long original;
    /**
     * The scheduler decrement passed time from original
     */
    private long back;
    /**
     * The scheduler period
     */
    private long period;

    /**
     * Time passed, useful to count how many minutes/hours or days has been
     * passed between the start and now and trigger the minute/hour and day triggers
     */
    private long passed;

    /**
     * Send a warning to the console when the timer is
     * cancelled because its owner has been unloaded
     */
    private boolean cancelUnloaded;
    /**
     * If the timer is cancelled
     */
    private boolean cancel;
    /**
     * If the timer is paused
     */
    private boolean pause;
    /**
     * If the timer should restart
     */
    private boolean restart;
    /**
     * If the timer is queued for restart
     */
    private boolean temp_restart;
    /**
     * If the timer has multi-threading
     */
    private boolean thread;

    /**
     * Action to perform when the timer gets paused
     */
    private Consumer<Long> pauseAction;
    /**
     * Action to perform when the timer gets cancelled
     */
    private Consumer<Long> cancelAction;

    /**
     * Initialize the scheduler
     *
     * @param owner the scheduler owner
     * @param time the scheduler start time
     * @param autoRestart if the scheduler should auto-restart
     *                    when it ends
     */
    public ModuleScheduler(final PluginModule owner, final Number time, final boolean autoRestart) {
        super(owner, SchedulerUnit.SECOND);
        period = 1L;

        cancelUnloaded = false;
        cancel = false;
        pause = false;
        temp_restart = false;
        thread = false;

        periodActions = new ConcurrentHashMap<>();
        secondsActions = new ConcurrentHashMap<>();
        secondsConsumer = new ConcurrentHashMap<>();
        periodConsumer = new ConcurrentHashMap<>();
        schedulerConsumer = new ConcurrentHashMap<>();

        onEndTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
        onStartTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
        onRestartTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

        pauseAction = null;
        cancelAction = null;

        module = owner;
        restart = autoRestart;

        switch (working_unit) {
            case MILLISECOND:
            case SECOND:
                original = time.longValue();
                back = time.longValue();
                break;
            default:
                //The maximum we should work with is seconds
                original = TimeUnit.SECONDS.convert(time.longValue(), SchedulerUnit.SECOND.toJavaUnit());
                back = TimeUnit.SECONDS.convert(time.longValue(), SchedulerUnit.SECOND.toJavaUnit());
                break;
        }

        id = getId();
        timersData.put(id, this);
    }

    /**
     * Initialize the scheduler
     *
     * @param owner the scheduler owner
     * @param builtId the scheduler ID
     * @throws TimerNotFound if the scheduler does not exist
     * @throws IllegalTimerAccess if the scheduler owner does not match with
     * provided
     */
    public ModuleScheduler(final PluginModule owner, final int builtId) throws TimerNotFound, IllegalTimerAccess {
        super(owner, SchedulerUnit.MILLISECOND);

        period = 1L;

        cancelUnloaded = false;
        cancel = false;
        pause = false;
        temp_restart = false;
        thread = false;

        periodActions = new ConcurrentHashMap<>();
        secondsActions = new ConcurrentHashMap<>();
        secondsConsumer = new ConcurrentHashMap<>();
        periodConsumer = new ConcurrentHashMap<>();
        schedulerConsumer = new ConcurrentHashMap<>();

        onEndTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
        onStartTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());
        onRestartTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

        pauseAction = null;
        cancelAction = null;

        SimpleScheduler built = timersData.getOrDefault(builtId, null);
        if (built != null) {
            if (built.getSource().isSource(owner)) {
                module = (PluginModule) built.getSource();
                restart = built.autoRestart();
                original = built.getOriginalTime();
                back = built.getMillis();
                id = builtId;
            } else {
                throw new IllegalTimerAccess(owner, built);
            }
        } else {
            throw new TimerNotFound(builtId);
        }
    }

    /**
     * Set the timer working unit.
     *
     * @param unit the working unit of the timer
     * @return this instance
     */
    public SimpleScheduler workingUnit(final SchedulerUnit unit) {
        //We must also change the timeout time and the original timeout time to match the new time unit
        if (!working_unit.equals(unit)) {
            switch (unit) {
                case MILLISECOND:
                    working_unit = SchedulerUnit.MILLISECOND;

                    //We know that if the time unit is not the same as current and we only work with ms and seconds, we must convert from seconds to ms
                    original = TimeUnit.MICROSECONDS.convert(original, TimeUnit.SECONDS);
                    back = TimeUnit.MICROSECONDS.convert(back, TimeUnit.SECONDS);
                    break;
                case SECOND:
                    working_unit = SchedulerUnit.SECOND;

                    //The same as above but instead of converting to ms, we convert to seconds
                    original = TimeUnit.SECONDS.convert(original, TimeUnit.MILLISECONDS);
                    back = TimeUnit.SECONDS.convert(back, TimeUnit.MILLISECONDS);
                    break;
                default:
                    //We must convert the current to the required unit and then to seconds.
                    original = unit.toJavaUnit().convert(original, (working_unit == SchedulerUnit.MILLISECOND ? TimeUnit.MILLISECONDS : TimeUnit.SECONDS));
                    back = unit.toJavaUnit().convert(back, (working_unit == SchedulerUnit.MILLISECOND ? TimeUnit.MILLISECONDS : TimeUnit.SECONDS));

                    //I may remove this line of code in the future as I think it's useless
                    original = TimeUnit.SECONDS.convert(original, unit.toJavaUnit());
                    back = TimeUnit.SECONDS.convert(back, unit.toJavaUnit());

                    working_unit = SchedulerUnit.SECOND;
                    break;
            }
        }

        return this;
    }

    /**
     * Notice when the timer has been stopped because
     * its source has been also unloaded
     *
     * @param status the notice unloaded status
     * @return this instance
     */
    public SimpleScheduler cancelUnloaded(final boolean status) {
        cancelUnloaded = status;
        return this;
    }

    /**
     * Cancel the scheduler
     */
    @Override
    public void cancel() {
        cancel = true;
    }

    /**
     * Pause the scheduler
     */
    @Override
    public void pause() {
        pause = true;
        if (pauseAction != null)
            runPeriodWithThread(pauseAction);
    }

    /**
     * Start the scheduler
     *
     * @throws TimerAlreadyStarted if the scheduler is already started
     */
    @Override
    public void start() throws TimerAlreadyStarted {
        Set<Integer> running = runningTimers.getOrDefault(module, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        if (!running.contains(id)) {
            ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

            AtomicInteger minutes = new AtomicInteger();
            AtomicInteger hours = new AtomicInteger();
            AtomicInteger days = new AtomicInteger();

            AtomicLong last_minute = new AtomicLong(0);
            AtomicLong last_hour = new AtomicLong(0);
            AtomicLong last_day = new AtomicLong(0);

            timer.scheduleAtFixedRate(() -> {
                if (ModuleLoader.isLoaded(module)) {
                    boolean run = (!cancelUnloaded || KarmaAPI.isLoaded(module));

                    long one_minute = (!working_unit.equals(SchedulerUnit.MILLISECOND) ? (working_unit.equals(SchedulerUnit.SECOND) ? working_unit : SchedulerUnit.SECOND) : SchedulerUnit.MILLISECOND).toJavaUnit().convert(1, TimeUnit.MINUTES);
                    long one_hour = (!working_unit.equals(SchedulerUnit.MILLISECOND) ? (working_unit.equals(SchedulerUnit.SECOND) ? working_unit : SchedulerUnit.SECOND) : SchedulerUnit.MILLISECOND).toJavaUnit().convert(1, TimeUnit.HOURS);
                    long one_day = (!working_unit.equals(SchedulerUnit.MILLISECOND) ? (working_unit.equals(SchedulerUnit.SECOND) ? working_unit : SchedulerUnit.SECOND) : SchedulerUnit.MILLISECOND).toJavaUnit().convert(1, TimeUnit.DAYS);
                    if (last_minute.get() + one_minute == passed) {
                        Set<Consumer<Integer>> minuteActions = schedulerConsumer.getOrDefault(SchedulerUnit.MINUTE, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                        for (Consumer<Integer> consumer : minuteActions) {
                            runWithThread(consumer, minutes.incrementAndGet());
                        }
                        last_minute.set(passed);
                    }
                    if (last_hour.get() + one_hour == passed) {
                        Set<Consumer<Integer>> hourActions = schedulerConsumer.getOrDefault(SchedulerUnit.HOUR, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                        for (Consumer<Integer> consumer : hourActions) {
                            runWithThread(consumer, hours.incrementAndGet());
                        }

                        last_hour.set(passed);
                    }
                    if (last_day.get() + one_day == passed) {
                        Set<Consumer<Integer>> dayActions = schedulerConsumer.getOrDefault(SchedulerUnit.DAY, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                        for (Consumer<Integer> consumer : dayActions) {
                            runWithThread(consumer, days.incrementAndGet());
                        }

                        last_day.set(passed);
                    }

                    //System.out.println(back);
                    if (run) {
                        if (!pause) {
                            if (cancel || temp_restart) {
                                if (!temp_restart) {
                                    timersData.remove(id);

                                    Set<Integer> ids = runningTimers.getOrDefault(module, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                                    ids.remove(id);

                                    runningTimers.put(module, ids);
                                    if (cancelAction != null)
                                        runPeriodWithThread(cancelAction);

                                    cancel = false;
                                    pause = false;
                                    temp_restart = false;

                                    timer.shutdown();
                                } else {
                                    back = original;
                                    onRestartTasks.forEach(this::runTaskWithThread);
                                    temp_restart = false;
                                }
                            } else {
                                executeTasks();

                                if (back == 0) {
                                    back = original;
                                    if (restart) {
                                        onRestartTasks.forEach(this::runTaskWithThread);
                                        back = original;
                                    } else {
                                        onEndTasks.forEach(this::runTaskWithThread);

                                        timersData.remove(id);
                                        Set<Integer> ids = runningTimers.getOrDefault(module, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                                        ids.remove(id);

                                        runningTimers.put(module, ids);

                                        cancel = false;
                                        pause = false;
                                        temp_restart = false;

                                        timer.shutdown();
                                    }
                                }

                                back--;
                            }
                        }

                        passed++;
                    } else {
                        timersData.remove(id);

                        Set<Integer> ids = runningTimers.getOrDefault(module, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                        ids.remove(id);

                        runningTimers.put(module, ids);
                        if (cancelAction != null)
                            runPeriodWithThread(cancelAction);

                        cancel = false;
                        pause = false;
                        temp_restart = false;

                        timer.shutdown();
                    }
                }
            }, 1, period, (!working_unit.equals(SchedulerUnit.MILLISECOND) ? (working_unit.equals(SchedulerUnit.SECOND) ? working_unit : SchedulerUnit.SECOND) : SchedulerUnit.MILLISECOND).toJavaUnit());
        } else {
            throw new TimerAlreadyStarted(this);
        }
    }

    /**
     * Restart the scheduler
     */
    @Override
    public void restart() {
        temp_restart = true;
    }

    /**
     * Set if the timer should auto restart
     * when it ends
     *
     * @param status if the timer should auto restart
     * @return this instance
     */
    @Override
    public SimpleScheduler updateAutoRestart(final boolean status) {
        restart = status;
        return this;
    }

    /**
     * Set the timer update period
     *
     * @param time the period
     * @return this instance
     */
    @Override
    public SimpleScheduler withPeriod(final Number time) {
        long unit = time.longValue();

        //Minimum of 250 ms ( 0,25 seconds )
        if (TimeUnit.MILLISECONDS.convert(unit, (working_unit == SchedulerUnit.MILLISECOND ? TimeUnit.MILLISECONDS : TimeUnit.SECONDS)) > 250) {
            period = unit;
        }

        return this;
    }

    /**
     * Set if the timer runs on another thread
     *
     * @param status if the timer has multi-threading
     * @return this instance
     */
    @Override
    public SimpleScheduler multiThreading(final boolean status) {
        thread = status;
        return this;
    }

    /**
     * Add an action to perform when the timer reaches
     * the specified time
     *
     * @param time the time
     * @param task the action to perform
     * @return this instance
     */
    @Override
    public SimpleScheduler exactAction(final long time, final Runnable task) {
        Set<Runnable> actions = periodActions.getOrDefault(time, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        actions.add(task);
        periodActions.put(time, actions);

        return this;
    }

    /**
     * Add an action when the timer passes a time
     *
     * @param action the action to perform
     * @return this instance
     */
    @Override
    public SimpleScheduler changeAction(final Consumer<Long> action) {
        long time = original;

        for (long i = 0; i <= original; i++) {
            Set<Consumer<Long>> actions = periodConsumer.getOrDefault(time, Collections.newSetFromMap(new ConcurrentHashMap<>()));
            actions.add(action);
            periodConsumer.put(time, actions);
            time--;
        }

        return this;
    }

    /**
     * Add an action when the timer passes a time
     * <p>
     * This will add a specific action when a specific time unit changes. This won't work with milliseconds
     * and or seconds, only with minutes, hours and days.
     *
     * @param action the action to perform
     * @param unit     the time unit
     * @return this instance
     */
    @Override
    public SimpleScheduler changeSpecificAction(final Consumer<Integer> action, final SchedulerUnit unit) {
        if (!unit.equals(SchedulerUnit.MILLISECOND) && !unit.equals(SchedulerUnit.SECOND)) {
            Set<Consumer<Integer>> actions = schedulerConsumer.getOrDefault(unit, Collections.newSetFromMap(new ConcurrentHashMap<>()));
            actions.add(action);

            schedulerConsumer.put(unit, actions);
        }

        return this;
    }

    /**
     * Add an action to perform when the timer reaches
     * the specified second
     *
     * @param time the second
     * @param task the action to perform
     * @return this instance
     * @deprecated Use {@link SimpleScheduler#exactAction(long, Runnable)} instead
     */
    @Override
    public @Deprecated SimpleScheduler exactSecondPeriodAction(final int time, final Runnable task) {
        Set<Runnable> actions = secondsActions.getOrDefault(time, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        actions.add(task);
        secondsActions.put(time, actions);
        return this;
    }

    /**
     * Add an action to perform when the timer reaches
     * the specified millisecond
     *
     * @param time the millisecond
     * @param task the action to perform
     * @return this instance
     * @deprecated Use {@link SimpleScheduler#changeAction(Consumer)} instead
     */
    @Override
    public @Deprecated SimpleScheduler exactPeriodAction(final long time, final Runnable task) {
        Set<Runnable> actions = periodActions.getOrDefault(time, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        actions.add(task);
        periodActions.put(time, actions);
        return this;
    }

    /**
     * Add an action when the timer passes a second
     *
     * @param action the action to perform
     * @return this instance
     * @deprecated Use {@link SimpleScheduler#changeAction(Consumer)} instead
     */
    @Override
    public @Deprecated SimpleScheduler secondChangeAction(final Consumer<Integer> action) {
        long second = TimeUnit.MILLISECONDS.toSeconds(original);
        while (second >= 0L) {
            final long millis = TimeUnit.SECONDS.toMillis(second--);
            final Set<Consumer<Integer>> actions = secondsConsumer.getOrDefault(millis, Collections.newSetFromMap(new ConcurrentHashMap<>()));
            actions.add(action);
            secondsConsumer.put(millis, actions);
        }
        return this;
    }

    /**
     * Add an action when the timer passes a millisecond
     *
     * @param action the action to perform
     * @return this instance
     * @deprecated Use {@link SimpleScheduler#changeAction(Consumer)} instead
     */
    @Override
    public @Deprecated SimpleScheduler periodChangeAction(final Consumer<Long> action) {
        long milli = original;
        while (milli >= 0L) {
            final Set<Consumer<Long>> actions = periodConsumer.getOrDefault(milli--, Collections.newSetFromMap(new ConcurrentHashMap<>()));
            actions.add(action);
            periodConsumer.put(milli, actions);
        }
        return this;
    }

    /**
     * Set the action to perform when the timer is cancelled
     *
     * @param action the action to perform
     * @return this instance
     */
    @Override
    public SimpleScheduler cancelAction(final Consumer<Long> action) {
        cancelAction = action;
        return this;
    }

    /**
     * Set the action to perform when the timer is paused
     *
     * @param action the action to perform
     * @return this instance
     */
    @Override
    public SimpleScheduler pauseAction(final Consumer<Long> action) {
        pauseAction = action;
        return this;
    }

    /**
     * Set the action to perform when the timer is started
     *
     * @param task the action to perform
     * @return this instance
     */
    @Override
    public SimpleScheduler startAction(final Runnable task) {
        onStartTasks.add(task);
        return this;
    }

    /**
     * Set the action to perform when the timer is
     * completely ended
     *
     * @param task the action to perform
     * @return this instance
     */
    @Override
    public SimpleScheduler endAction(final Runnable task) {
        onEndTasks.add(task);
        return this;
    }

    /**
     * Set the action to perform when the timer is restarted
     *
     * @param task the action to perform
     * @return this instance
     */
    @Override
    public SimpleScheduler restartAction(final Runnable task) {
        onRestartTasks.add(task);
        return this;
    }

    /**
     * Add a conditional action
     *
     * @param condition          the condition that the timer
     *                           must complete
     * @param condition_value           the time
     * @param action             the action to perform
     * @return this instance
     */
    @Override
    public SimpleScheduler condition(final TimeCondition condition, final long condition_value, final Consumer<Long> action) {
        switch (condition) {
            case EQUALS: {
                Set<Consumer<Long>> actions = periodConsumer.getOrDefault(condition_value, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                actions.add(action);
                periodConsumer.put(condition_value, actions);
                break;
            }
            case OVER_OF: {
                long c_over_val = condition_value;
                while (c_over_val <= original) {
                    Set<Consumer<Long>> actions = periodConsumer.getOrDefault(c_over_val++, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    actions.add(action);
                    periodConsumer.put(c_over_val, actions);
                }
                break;
            }
            case MINUS_TO: {
                long c_minus_val = condition_value;
                while (c_minus_val >= 0L) {
                    Set<Consumer<Long>> actions = periodConsumer.getOrDefault(c_minus_val--, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    actions.add(action);
                    periodConsumer.put(c_minus_val, actions);
                }
                break;
            }
        }

        return this;
    }

    /**
     * Add a conditional action
     *
     * @param condition the condition that the timer
     *                           must complete
     * @param condition_value the timer second
     * @param action the action to perform
     * @return this instance
     * @deprecated Use {@link SimpleScheduler#condition(TimeCondition, long, Consumer)} instead
     */
    @Override
    public @Deprecated SimpleScheduler conditionalAction(final TimeCondition condition, final int condition_value, final Consumer<Integer> action) {
        switch (condition) {
            case EQUALS: {
                final Set<Consumer<Integer>> actions = secondsConsumer.getOrDefault((working_unit.toJavaUnit() == TimeUnit.SECONDS ? TimeUnit.SECONDS.toMillis(condition_value) : condition_value), Collections.newSetFromMap(new ConcurrentHashMap<>()));
                actions.add(action);
                secondsConsumer.put(TimeUnit.SECONDS.toMillis(condition_value), actions);
                break;
            }
            case OVER_OF: {
                long c_over_val = condition_value;
                while (c_over_val <= original) {
                    final Set<Consumer<Integer>> actions = secondsConsumer.getOrDefault((working_unit.toJavaUnit() == TimeUnit.SECONDS ? TimeUnit.SECONDS.toMillis(c_over_val++) : c_over_val++), Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    actions.add(action);
                    secondsConsumer.put(c_over_val, actions);
                }
                break;
            }
            case MINUS_TO: {
                long c_minus_val = condition_value;
                while (c_minus_val >= 0L) {
                    final Set<Consumer<Integer>> actions = secondsConsumer.getOrDefault((working_unit.toJavaUnit() == TimeUnit.SECONDS ? TimeUnit.SECONDS.toMillis(c_minus_val--) : c_minus_val--), Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    actions.add(action);
                    secondsConsumer.put(c_minus_val, actions);
                }
                break;
            }
        }

        return this;
    }

    /**
     * Add a conditional action
     *
     * @param condition the condition that the timer must complete
     * @param condition_value the timer millisecond
     * @param action the action to perform
     * @return this instance
     * @deprecated Use {@link SimpleScheduler#condition(TimeCondition, long, Consumer)} instead
     */
    @Override
    public @Deprecated SimpleScheduler conditionalPeriodAction(final TimeCondition condition, final long condition_value, final Consumer<Long> action) {
        switch (condition) {
            case EQUALS: {
                final Set<Consumer<Long>> actions = periodConsumer.getOrDefault(condition_value, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                actions.add(action);
                periodConsumer.put(TimeUnit.SECONDS.toMillis(condition_value), actions);
                break;
            }
            case OVER_OF: {
                long c_over_val = condition_value;
                while (c_over_val <= original) {
                    final Set<Consumer<Long>> actions = periodConsumer.getOrDefault(c_over_val++, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    actions.add(action);
                    periodConsumer.put(c_over_val, actions);
                }
                break;
            }
            case MINUS_TO: {
                long c_minus_val = condition_value;
                while (c_minus_val >= 0L) {
                    final Set<Consumer<Long>> actions = periodConsumer.getOrDefault(c_minus_val--, Collections.newSetFromMap(new ConcurrentHashMap<>()));
                    actions.add(action);
                    periodConsumer.put(c_minus_val, actions);
                }
                break;
            }
        }

        return this;
    }

    /**
     * Get if the timer is cancelled
     *
     * @return if the timer is cancelled
     */
    @Override
    public boolean isCancelled() {
        return cancel;
    }

    /**
     * Get if the timer is running
     *
     * @return if the timer is running
     */
    @Override
    public boolean isRunning() {
        Set<Integer> ids = runningTimers.getOrDefault(module, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        return ids.contains(id);
    }

    /**
     * Get if the timer is paused
     *
     * @return if the timer is paused
     */
    @Override
    public boolean isPaused() {
        return pause;
    }

    /**
     * Get if the timer auto restarts
     *
     * @return if the timer starts the timer
     * automatically when it ends
     */
    @Override
    public boolean autoRestart() {
        return restart;
    }

    /**
     * Get if the timer has multi-threading enabled
     *
     * @return if the timer runs on another thread
     */
    @Override
    public boolean isMultiThreading() {
        return thread;
    }

    /**
     * Get the timer start time
     *
     * @return the timer start time
     */
    @Override
    public long getOriginalTime() {
        return original;
    }

    /**
     * Get the timer configured period
     *
     * @return the timer update period
     */
    @Override
    public long getPeriod() {
        return period;
    }

    /**
     * Get the timer milliseconds
     *
     * @return the timer exact time
     */
    @Override
    public long getMillis() {
        return back;
    }

    /**
     * Execute the tasks corresponding to the current
     * second/millisecond
     */
    private void executeTasks() {
        Set<Consumer<Long>> periodConsumers = periodConsumer.getOrDefault(back, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        Set<Consumer<Integer>> secondConsumers = secondsConsumer.getOrDefault(back, Collections.newSetFromMap(new ConcurrentHashMap<>()));

        Set<Runnable> runnables = periodActions.getOrDefault(back, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        Set<Runnable> secondRunnable = secondsActions.getOrDefault((int) (working_unit.toJavaUnit() == TimeUnit.MILLISECONDS ? TimeUnit.MILLISECONDS.toSeconds(back) : back), Collections.newSetFromMap(new ConcurrentHashMap<>()));

        runnables.addAll(secondRunnable);

        for (Consumer<Long> consumer : periodConsumers)
            runPeriodWithThread(consumer);
        for (Consumer<Integer> consumer : secondConsumers)
            runSecondsWithThread(consumer);
        for (Runnable runnable : runnables)
            runTaskWithThread(runnable);
    }

    /**
     * Run a seconds task corresponding the current
     * thread configuration
     *
     * @param task the task to run
     * @param value the int value
     */
    private void runWithThread(final Consumer<Integer> task, final int value) {
        if (thread) {
            (new Thread(() -> task.accept(value))).start();
        } else {
            task.accept(value);
        }
    }

    /**
     * Run a seconds task corresponding the current
     * thread configuration
     *
     * @param task the task to run
     */
    private void runSecondsWithThread(final Consumer<Integer> task) {
        if (thread) {
            (new Thread(() -> task.accept((int) (working_unit.toJavaUnit() == TimeUnit.MILLISECONDS ? TimeUnit.MILLISECONDS.toSeconds(back) : back)))).start();
        } else {
            task.accept((int) TimeUnit.MILLISECONDS.toSeconds(back));
        }
    }

    /**
     * Run a milliseconds task corresponding the current
     * thread configuration
     *
     * @param task the task to run
     */
    private void runPeriodWithThread(final Consumer<Long> task) {
        if (thread) {
            (new Thread(() -> task.accept(back))).start();
        } else {
            task.accept(back);
        }
    }

    /**
     * Run a simple task corresponding the current
     * thread configuration
     *
     * @param task the task to run
     */
    private void runTaskWithThread(final Runnable task) {
        if (thread) {
            (new Thread(task)).start();
        } else {
            task.run();
        }
    }
}
