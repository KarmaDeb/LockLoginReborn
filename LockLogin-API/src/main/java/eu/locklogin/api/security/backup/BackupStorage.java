package eu.locklogin.api.security.backup;

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.security.backup.data.AccountBackup;

import java.time.Instant;

/**
 * A storage of all account backups
 */
public interface BackupStorage extends Comparable<BackupStorage> {

    /**
     * Get the backup id
     *
     * @return the backup id
     */
    String id();

    /**
     * Get all the backup accounts
     *
     * @return the account backups
     */
    AccountBackup[] getAccounts();

    /**
     * Get an account from its account ID
     *
     * @param id the id to find with
     * @return the account
     */
    AccountBackup find(final AccountID id);

    /**
     * Get when the backup was created
     *
     * @return the backup creation time
     */
    Instant creation();

    /**
     * Get all the backup accounts
     *
     * @return the backup accounts
     */
    int accounts();

    /**
     * Destroy the backup
     *
     * @return if the backup could be destroyed
     */
    boolean destroy();
}
