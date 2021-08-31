package eu.locklogin.api.file.options;

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

import eu.locklogin.api.util.enums.UpdateChannel;

import java.util.concurrent.TimeUnit;

/**
 * Updater configuration
 */
public final class UpdaterConfig {

    private final UpdateChannel channel;
    private final boolean enabled;
    private final int time;

    /**
     * Initialize the updater configuration
     *
     * @param updateChannel the update channel
     * @param chekUpdates   enable the update checker
     * @param checkTime     periodical update checks time
     *                      interval
     */
    public UpdaterConfig(final String updateChannel, final boolean chekUpdates, final int checkTime) {
        channel = UpdateChannel.valueOf(updateChannel);
        enabled = chekUpdates;
        time = checkTime;
    }

    /**
     * Get the update channel
     *
     * @return the update channel
     */
    public UpdateChannel getChannel() {
        return channel;
    }

    /**
     * Check if the update checker is enabled
     *
     * @return if the update checker is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the update check interval
     *
     * @return the update check interval
     */
    public int getInterval() {
        return (int) TimeUnit.MINUTES.toSeconds(time);
    }
}
