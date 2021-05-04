package ml.karmaconfigs.locklogin.api.files.options;

import java.util.concurrent.TimeUnit;

public class BruteForceConfig {

    private final int tries;
    private final int time;

    /**
     * Initialize brute force protection configuration
     *
     * @param maxTries  the maximum amount of tries before
     *                  activating brute force on a player
     * @param blockTime the blocking time
     */
    public BruteForceConfig(final int maxTries, final int blockTime) {
        tries = maxTries;
        time = blockTime;
    }

    public final int getMaxTries() {
        return tries;
    }

    /**
     * Get the amount of time the player will be
     * blocked
     *
     * @return the amount of time the player will
     * be blocked ( in seconds )
     */
    public final int getBlockTime() {
        return (int) TimeUnit.MINUTES.toSeconds(time);
    }
}
