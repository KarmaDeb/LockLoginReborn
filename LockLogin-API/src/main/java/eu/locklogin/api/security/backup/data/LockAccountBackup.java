package eu.locklogin.api.security.backup.data;

import eu.locklogin.api.account.AccountID;

import java.time.Instant;

/**
 * Locked account backup
 */
public interface LockAccountBackup {

    /**
     * Get the lock issuer
     *
     * @return the issuer
     */
    String issuer();

    /**
     * Get the lock date
     *
     * @return the lock date
     */
    Instant date();
}
