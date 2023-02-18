package eu.locklogin.api.security.backup;

import ml.karmaconfigs.api.common.timer.scheduler.BiLateScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;

import java.time.Instant;

/**
 * Backup scheduler
 */
public interface BackupScheduler {

    /**
     * Perform a backup
     *
     * @return the backup id
     */
    LateScheduler<String> performBackup();

    /**
     * Perform a backup
     *
     * @param id the backup id
     * @return if the backup was able to be created
     */
    LateScheduler<Boolean> performBackup(final String id);

    /**
     * Fetch all the backups
     *
     * @return all the backups
     */
    LateScheduler<BackupStorage[]> fetchAll();

    /**
     * Fetch a backup
     *
     * @param id the backup id
     * @return the backup
     */
    LateScheduler<BackupStorage> fetch(final String id);

    /**
     * Purge all the backups behind the provided date
     *
     * @param limit the purge limit
     * @return the removed backups
     */
    LateScheduler<Integer> purge(final Instant limit);

    /**
     * Destroy all the backups that are between
     * the provided instants
     *
     * @param from the minimum
     * @param to the maximum
     * @return the removed backups
     */
    LateScheduler<Integer> purgeBetween(final Instant from, final Instant to);

    /**
     * Destroy all the backups above the provided instant
     *
     * @param start the time to start
     * @return the removed backups
     */
    LateScheduler<Integer> purgeFrom(final Instant start);

    /**
     * Restore a single backup
     *
     * @param storage the backup to restore
     * @param force force the backup. If true (default), all data will be replaced
     * @return if the backup was able to restore and
     * the restored accounts
     */
    BiLateScheduler<Boolean, Integer> restore(final BackupStorage storage, final boolean force);

    /**
     * Restore from a backup to another
     *
     * @param from the backup to start from
     * @param to the backup to store until
     * @param force force the backup. If true (default), all data will be replaced
     * @return if the backup was able to restore and
     * the restored accounts
     */
    BiLateScheduler<Boolean, Integer> restore(final BackupStorage from, final BackupStorage to, final boolean force);

    /**
     * Restore all the backups starting from
     * the specified one
     *
     * @param from the backup to start from
     * @param force force the backup. If true (default), all data will be replaced
     * @return if the backup was able to restore and
     * the restored accounts
     */
    BiLateScheduler<Boolean, Integer> restoreAllFrom(final BackupStorage from, final boolean force);

    /**
     * Restore all the backups until the
     * specified one
     *
     * @param to the backup to stop on
     * @param force force the backup. If true (default), all data will be replaced
     * @return if the backup was able to restore and
     * the restored accounts
     */
    BiLateScheduler<Boolean, Integer> restoreAllTo(final BackupStorage to, final boolean force);
}
