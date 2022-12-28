package eu.locklogin.api.common.session;

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.BruteForce;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.boss.BossColor;
import ml.karmaconfigs.api.common.boss.BossProvider;
import ml.karmaconfigs.api.common.karma.KarmaSource;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.string.StringUtils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionCheck<T> implements Runnable {

    private final static Set<UUID> under_check = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final static Set<UUID> cancel_queue = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final static Set<UUID> restart_queue = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ModulePlayer player;
    private final KarmaSource source;
    private final BossProvider<T> boss;
    private String BAR_COLOR = "&a";
    private Runnable onEnd;

    public SessionCheck(final KarmaSource owner, final ModulePlayer client, final BossProvider<T> provider) {
        player = client;
        source = owner;
        boss = provider;
    }

    public SessionCheck<T> whenComplete(final Runnable task) {
        onEnd = task;

        return this;
    }

    /**
     * When an object implementing interface {@code Runnable} is used
     * to create a thread, starting the thread causes the object's
     * {@code run} method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method {@code run} is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if (!under_check.contains(player.getUUID())) {
            under_check.add(player.getUUID());

            PluginConfiguration config = CurrentPlatform.getConfiguration();
            PluginMessages messages = CurrentPlatform.getMessages();
            SessionKeeper keeper = new SessionKeeper(player);

            BruteForce protection = new BruteForce(player.getAddress());

            if (!protection.isPanicking(player.getUUID()) && keeper.hasSession() && keeper.isOwner(player.getAddress()) && config.enableSessions()) {
                player.requestLogin();

                if (onEnd != null)
                    onEnd.run();

                if (boss != null)
                    boss.cancel();

                under_check.remove(player.getUUID());
                keeper.destroy();
            } else {
                int tmp_time = config.registerOptions().timeOut();
                if (player.getAccount().isRegistered()) {
                    tmp_time = config.loginOptions().timeOut();
                    if (protection.isPanicking(player.getUUID())) {
                        player.sendMessage(messages.prefix() + messages.panicLogin());
                        boss.update(messages.panicLogin(), true);
                    } else {
                        player.sendMessage(messages.prefix() + messages.login());
                    }

                    if (config.loginOptions().hasBossBar())
                        boss.displayTime(tmp_time);
                } else {
                    player.sendMessage(messages.prefix() + messages.register());

                    if (config.registerOptions().hasBossBar())
                        boss.displayTime(tmp_time);
                }

                if (boss != null) {
                    boss.scheduleBar(Collections.singletonList(player.getPlayer()));
                }

                int time = tmp_time;
                AccountManager manager = player.getAccount();
                SimpleScheduler timer = new SourceScheduler(source, time, SchedulerUnit.SECOND, false).multiThreading(false);
                timer.changeAction((timer_time) -> {
                    if (!cancel_queue.contains(player.getUUID())) {
                        ClientSession session = player.getSession();
                        if (!session.isLogged()) {
                            if (boss != null) {
                                if (timer_time == ((int) Math.round(((double) time / 2)))) {
                                    boss.color(BossColor.YELLOW);
                                    BAR_COLOR = "&e";
                                }

                                if (timer_time == ((int) Math.round(((double) time / 3)))) {
                                    boss.color(BossColor.RED);
                                    BAR_COLOR = "&eu.c";
                                }

                                if (manager.isRegistered()) {
                                    if (protection.isPanicking(player.getUUID())) {
                                        boss.update(messages.panicLogin(), false);
                                    } else {
                                        boss.update(messages.loginBar(BAR_COLOR, timer_time), false);
                                    }
                                } else {
                                    boss.update(messages.registerBar(BAR_COLOR, timer_time), false);
                                }
                            }

                            if (manager.isRegistered()) {
                                String title = messages.loginTitle(timer_time);
                                String subtitle = messages.loginSubtitle(timer_time);
                                if (protection.isPanicking(player.getUUID())) {
                                    title = messages.panicTitle();
                                    subtitle = messages.panicSubtitle();
                                }

                                if (!StringUtils.isNullOrEmpty(title) || !StringUtils.isNullOrEmpty(subtitle)) {
                                    player.sendTitle(title, subtitle, 0, 5, 0);
                                }
                            } else {
                                if (!StringUtils.isNullOrEmpty(messages.registerTitle(timer_time)) || !StringUtils.isNullOrEmpty(messages.registerSubtitle(timer_time)))
                                    player.sendTitle(messages.registerTitle(timer_time), messages.registerSubtitle(timer_time), 0, 5, 0);
                            }
                        } else {
                            timer.cancel();
                            keeper.destroy();
                        }
                    } else {
                        timer.cancel();
                        cancel_queue.remove(player.getUUID());
                        keeper.destroy();
                    }
                }).endAction(() -> {
                    if (restart_queue.contains(player.getUUID())) {
                        restart_queue.remove(player.getUUID());
                        run();
                    } else {
                        if (onEnd != null)
                            onEnd.run();

                        if (boss != null)
                            boss.cancel();

                        ClientSession session = player.getSession();

                        if (!session.isLogged())
                            player.requestKick((manager.isRegistered() ? (protection.isPanicking(player.getUUID()) ? messages.panicMode() : messages.loginTimeOut()) : messages.registerTimeOut()));

                        under_check.remove(player.getUUID());
                        keeper.destroy();
                    }
                }).cancelAction((cancelTime) -> {
                    if (restart_queue.contains(player.getUUID())) {
                        restart_queue.remove(player.getUUID());
                        run();
                    } else {
                        if (onEnd != null)
                            onEnd.run();

                        if (boss != null)
                            boss.cancel();

                        ClientSession session = player.getSession();

                        if (!session.isLogged())
                            player.requestKick((manager.isRegistered() ? (protection.isPanicking(player.getUUID()) ? messages.panicMode() : messages.loginTimeOut()) : messages.registerTimeOut()));

                        under_check.remove(player.getUUID());
                        keeper.destroy();
                    }
                });

                timer.start();
                startMessageTask();
            }
        }
    }

    /**
     * Initialize the session message
     * task
     */
    private void startMessageTask() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        PluginMessages messages = CurrentPlatform.getMessages();
        AccountManager manager = player.getAccount();
        ClientSession session = player.getSession();

        int time = config.registerOptions().getMessageInterval();
        if (manager.isRegistered())
            time = config.loginOptions().getMessageInterval();

        BruteForce protection = new BruteForce(player.getAddress());

        SimpleScheduler timer = new SourceScheduler(source, time, SchedulerUnit.SECOND, true);
        timer.restartAction(() -> {
            if (under_check.contains(player.getUUID())) {
                if (!session.isLogged()) {
                    if (manager.isRegistered()) {
                        if (protection.isPanicking(player.getUUID())) {
                            player.sendMessage(messages.prefix() + messages.panicLogin());
                        } else {
                            player.sendMessage(messages.prefix() + messages.login());
                        }
                    } else {
                        player.sendMessage(messages.prefix() + messages.register());
                    }
                }
            } else {
                timer.cancel();
            }
        });
        timer.start();
    }

    /**
     * Cancel the player session check
     */
    public void cancelCheck() {
        cancel_queue.add(player.getUUID());
    }

    /**
     * Restart the player checker
     */
    public void restart() {
        cancel_queue.add(player.getUUID());
        restart_queue.add(player.getUUID());
    }

    /**
     * Get if the player is under a check
     *
     * @return if the player is under session check
     */
    public boolean isUnderCheck() {
        return under_check.contains(player.getUUID());
    }
}
