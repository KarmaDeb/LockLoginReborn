package ml.karmaconfigs.locklogin.plugin.common.security;

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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * LockLogin brute force protection
 */
public final class BruteForce {

    private final static Map<InetAddress, Integer> tries = new HashMap<>();
    private final static Map<InetAddress, Long> block_time = new HashMap<>();

    private final InetAddress ip;

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
    public final void fail() {
        tries.put(ip, tries() + 1);
    }

    /**
     * Success the login
     */
    public final void success() {
        tries.remove(ip);
    }

    /**
     * Block the account
     */
    public final void block(final int time) {
        block_time.put(ip, (long) time);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (getBlockLeft() > 0) {
                    block_time.put(ip, getBlockLeft() - 1);
                } else {
                    block_time.remove(ip);
                    timer.cancel();
                }
            }
        }, 0, 1000);
    }

    /**
     * Get the failed login tries the uuid has
     *
     * @return the failed login tries the account has
     */
    public final int tries() {
        return tries.getOrDefault(ip, 0);
    }

    /**
     * Get the uuid block time left
     *
     * @return the uuid block time left
     */
    public final long getBlockLeft() {
        return block_time.getOrDefault(ip, 0L);
    }

    /**
     * Get if the uuid is blocked
     *
     * @return if the uuid is blocked
     */
    public final boolean isBlocked() {
        return block_time.getOrDefault(ip, 0L) > 1L;
    }
}
