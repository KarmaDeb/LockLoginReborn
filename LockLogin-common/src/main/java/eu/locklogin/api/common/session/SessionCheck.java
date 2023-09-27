package eu.locklogin.api.common.session;

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.BruteForce;
import eu.locklogin.api.common.session.persistence.SessionKeeper;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.minecraft.boss.BossColor;
import ml.karmaconfigs.api.common.minecraft.boss.BossProvider;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionCheck<T> implements Runnable {

    private final static Map<UUID, SessionCheck<?>> schedulers = new ConcurrentHashMap<>();
    private final static Set<UUID> restart_queue = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ModulePlayer player;
    private final KarmaSource source;
    private final BossProvider<T> boss;

    private SimpleScheduler mainTask;
    private SimpleScheduler messageTask;

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
        if (!schedulers.containsKey(player.getUUID())) {
            schedulers.put(player.getUUID(), this);

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

                schedulers.remove(player.getUUID());
                keeper.destroy();
            } else {
                int tmp_time = config.registerOptions().timeOut();
                if (player.getAccount().isRegistered()) {
                    tmp_time = config.loginOptions().timeOut();
                    if (protection.isPanicking(player.getUUID())) {
                        player.sendMessage(messages.prefix() + messages.panicLogin());
                        boss.update(messages.panicLogin(), true);
                    } else {
                        if (!player.getSession().isLogged()) {
                            player.sendMessage(messages.prefix() + messages.login());
                        }
                    }

                    if (config.loginOptions().hasBossBar())
                        boss.displayTime(tmp_time);
                } else {
                    if (!player.getSession().isLogged()) {
                        player.sendMessage(messages.prefix() + messages.register());
                    }

                    if (config.registerOptions().hasBossBar())
                        boss.displayTime(tmp_time);
                }

                if (boss != null && !player.getSession().isLogged()) {
                    boss.scheduleBar(Collections.singletonList(player.getPlayer()));
                }

                if (player.getSession().isLogged() && boss != null) {
                    boss.cancel();
                }

                int time = tmp_time;
                AccountManager manager = player.getAccount();
                mainTask = new SourceScheduler(source, time, SchedulerUnit.SECOND, false).multiThreading(false);
                mainTask.changeAction((timer_time) -> {
                    SessionCheck<?> stored = schedulers.getOrDefault(player.getUUID(), null);
                    if (stored != null && stored.mainTask.getId() == mainTask.getId()) {
                        ClientSession session = player.getSession();
                        if (!session.isLogged()) {
                            if (boss != null) {
                                if (timer_time == ((int) Math.round(((double) time / 2)))) {
                                    boss.color(BossColor.YELLOW);
                                    BAR_COLOR = "&e";
                                }

                                if (timer_time == ((int) Math.round(((double) time / 3)))) {
                                    boss.color(BossColor.RED);
                                    BAR_COLOR = "&c";
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
                            mainTask.cancel();
                            keeper.destroy();
                        }
                    }
                }).endAction(() -> {
                    if (restart_queue.contains(player.getUUID())) {
                        restart_queue.remove(player.getUUID());
                        run();
                    } else {
                        SessionCheck<?> stored = schedulers.getOrDefault(player.getUUID(), null);
                        if (stored != null && stored.mainTask.getId() == mainTask.getId()) {
                            if (onEnd != null)
                                onEnd.run();

                            if (boss != null)
                                boss.cancel();

                            ClientSession session = player.getSession();

                            if (!session.isLogged())
                                player.requestKick((manager.isRegistered() ? (protection.isPanicking(player.getUUID()) ? messages.panicMode() : messages.loginTimeOut()) : messages.registerTimeOut()));

                            keeper.destroy();
                        }
                    }
                }).cancelAction((cancelTime) -> {
                    if (restart_queue.contains(player.getUUID())) {
                        restart_queue.remove(player.getUUID());
                        run();
                    } else {
                        SessionCheck<?> stored = schedulers.getOrDefault(player.getUUID(), null);
                        if (stored != null && stored.mainTask.getId() == mainTask.getId()) {
                            if (onEnd != null)
                                onEnd.run();

                            if (boss != null)
                                boss.cancel();

                            ClientSession session = player.getSession();

                            if (!session.isLogged())
                                player.requestKick((manager.isRegistered() ? (protection.isPanicking(player.getUUID()) ? messages.panicMode() : messages.loginTimeOut()) : messages.registerTimeOut()));

                            keeper.destroy();
                        }
                    }
                });

                mainTask.start();
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

        messageTask = new SourceScheduler(source, time, SchedulerUnit.SECOND, true);
        messageTask.restartAction(() -> {
            SessionCheck<?> stored = schedulers.getOrDefault(player.getUUID(), null);
            if (!player.isPlaying() || (stored != null && stored.messageTask.getId() != messageTask.getId())) {
                cancelCheck("Player went offline");

                if (stored != null) {
                    stored.mainTask.cancel();
                    stored.messageTask.cancel();
                    return;
                }

                return; //Stop execution here
            }

            if (schedulers.containsKey(player.getUUID())) {
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
                messageTask.cancel();
            }
        });

        messageTask.start();
    }

    /**
     * Cancel the player session check
     */
    public void cancelCheck(final String reason) {
        try {
            SessionCheck<?> sc = schedulers.remove(player.getUUID());
            source.logger().scheduleLog(Level.INFO, "Cancelled session check scheduler of {0} ({1}) for {2}", player.getName(), player.getUUID(), reason);

            sc.mainTask.cancel();
            sc.messageTask.cancel();
        } catch (NullPointerException ignored) {}
    }

    /**
     * Restart the player checker
     */
     @SuppressWarnings("unused")
    public void restart() {
        restart_queue.add(player.getUUID());
    }

    /**
     * Get if the player is under a check
     *
     * @return if the player is under session check
     */
    public boolean isUnderCheck() {
        return schedulers.containsKey(player.getUUID());
    }
}
