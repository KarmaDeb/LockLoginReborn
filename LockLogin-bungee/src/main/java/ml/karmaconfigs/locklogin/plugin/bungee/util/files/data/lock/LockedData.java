package ml.karmaconfigs.locklogin.plugin.bungee.util.files.data.lock;

import java.time.Instant;

public final class LockedData {

    private final String issuer;
    private final Instant date;
    private final boolean locked;

    /**
     * Initialize the lock data
     *
     * @param administrator the administrator who
     *                      issued the lock
     * @param lockDate      the date when the lock was
     *                      performed
     * @param exists        if the data exists
     */
    public LockedData(final String administrator, final Instant lockDate, final boolean exists) {
        issuer = administrator;
        date = lockDate;
        locked = exists;
    }

    /**
     * Get the lock administrator
     *
     * @return the lock administrator
     */
    public final String getAdministrator() {
        return issuer;
    }

    /**
     * Get when the lock was performed
     *
     * @return the lock instant
     */
    public final Instant getLockDate() {
        return date;
    }

    /**
     * Get if the lock data exists
     *
     * @return if the lock data exists
     */
    public final boolean isLocked() {
        return locked;
    }
}
