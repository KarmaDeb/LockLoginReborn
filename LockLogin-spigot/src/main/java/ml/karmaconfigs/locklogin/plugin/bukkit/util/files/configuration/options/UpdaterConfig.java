package ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.options;

import ml.karmaconfigs.locklogin.plugin.common.utils.enums.UpdateChannel;

import java.util.concurrent.TimeUnit;

public final class UpdaterConfig {

    private final UpdateChannel channel;
    private final boolean enabled;
    private final int time;

    /**
     * Initialize the updater configuration
     *
     * @param updateChannel the update channel
     * @param chekUpdates enable the update checker
     * @param checkTime periodical update checks time
     *                  interval
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
    public final UpdateChannel getChannel() {
        return channel;
    }

    /**
     * Check if the update checker is enabled
     *
     * @return if the update checker is enabled
     */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the update check interval
     *
     * @return the update check interval
     */
    public final int getInterval() {
        return (int) TimeUnit.MINUTES.toSeconds(time);
    }
}
