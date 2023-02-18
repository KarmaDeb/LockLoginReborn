package eu.locklogin.api.common.security;

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

import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * LockLogin brute force protection
 */
public final class BruteForce {

    private final static KarmaSource plugin = APISource.loadProvider("LockLogin");
    private final static Map<InetAddress, Integer> tries = new HashMap<>();
    private final static Map<InetAddress, BruteForce> blocked = new HashMap<>();

    private final static Set<UUID> panicking = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final InetAddress ip;

    private SimpleScheduler scheduler;

    /**
     * Initialize the brute force system
     *
     * @param address the account id
     */
    public BruteForce(final InetAddress address) {
        ip = address;
    }

    /**
     * Fail the login
     */
    public void fail() {
        tries.put(ip, tries() + 1);
    }

    /**
     * Success the login
     */
    public void success() {
        tries.remove(ip);
    }

    /**
     * Block the account
     *
     * @param time the block time
     */
    public void block(final int time) {
        /*if (time > 0) {
            if (!block_time.containsKey(ip)) {
                block_time.put(ip, (long) time);

                SimpleScheduler scheduler = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true);
                scheduler.restartAction(() -> {
                    long time_left = block_time.getOrDefault(ip, 0L);
                    if (time_left > 0) {
                        block_time.put(ip, time_left - 1);
                    } else {
                        block_time.remove(ip);
                        scheduler.cancel();
                    }
                });

                scheduler.start();
            }
        } else {
            block_time.remove(ip);
        }*/
        if (blocked.containsKey(ip)) {
            SimpleScheduler stored = blocked.get(ip).scheduler;
            if (stored != null)
                stored.cancel();
        }

        if (time > 0) {
            scheduler = new SourceScheduler(plugin, time, SchedulerUnit.MINUTE, false);
            scheduler.endAction(() -> {
                blocked.remove(ip);
                tries.remove(ip);
            }).start();

            blocked.put(ip, this);
        }
    }

    /**
     * Set the account in panic mode
     *
     * @param id the account id
     */
    public void panic(final UUID id) {
        panicking.add(id);
    }

    /**
     * Remove the account from panic mode
     *
     * @param id the account id
     */
    public void unPanic(final UUID id) {
        panicking.remove(id);
    }

    /**
     * Get the failed login tries the uuid has
     *
     * @return the failed login tries the account has
     */
    public int tries() {
        return tries.getOrDefault(ip, 0);
    }

    /**
     * Get the uuid block time left
     *
     * @return the uuid block time left
     */
    public long getBlockLeft() {
        return (scheduler != null ? TimeUnit.MILLISECONDS.toSeconds(scheduler.getMillis()) : 0);
    }

    /**
     * Get if the uuid is blocked
     *
     * @return if the uuid is blocked
     */
    public boolean isBlocked() {
        return scheduler != null && scheduler.isRunning();
    }

    /**
     * Get if the account is panicking
     *
     * @param id the account id
     * @return if the account is in panic mode
     */
    public boolean isPanicking(final UUID id) {
        return panicking.contains(id);
    }
}
