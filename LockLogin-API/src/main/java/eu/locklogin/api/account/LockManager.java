package eu.locklogin.api.account;

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

import java.time.Instant;

/**
 * LockLogin locked account
 */
public abstract class LockManager {

    private final AccountID id;

    /**
     * Initialize the locked account
     *
     * @param accountID the account ID to manage
     */
    public LockManager(final AccountID accountID) {
        id = accountID;
    }

    /**
     * Lock the account
     */
    public abstract void lock(final String administrator);

    /**
     * Release the account
     *
     * @return if the account could be released
     */
    public abstract boolean release();

    /**
     * Get the lock issuer
     *
     * @return the lock issuer
     */
    public abstract String getIssuer();

    /**
     * Get the lock date
     *
     * @return the lock date
     */
    public abstract Instant getLockDate();

    /**
     * Get if the account is locked
     *
     * @return if the account is locked
     */
    public abstract boolean isLocked();

    /**
     * Get the account ID
     *
     * @return the account ID
     */
    public final AccountID getId() {
        return id;
    }
}
