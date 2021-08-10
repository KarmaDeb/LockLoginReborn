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

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

/**
 * LockLogin account manager
 * <p>
 * MUST HAVE AN EMPTY CONSTRUCTOR
 */
public abstract class AccountManager implements Serializable {

    /**
     * Check if the file exists
     *
     * @return if the file exists
     */
    public abstract boolean exists();

    /**
     * Tries to create the account
     *
     * @return if the account could be created
     */
    public abstract boolean create();

    /**
     * Tries to remove the account
     *
     * @param issuer the account removal issuer
     * @return if the account could be removed
     */
    public abstract boolean remove(final String issuer);

    /**
     * Save the account id
     *
     * @param id the account id
     */
    public abstract void saveUUID(final AccountID id);

    /**
     * Save the account 2FA status
     *
     * @param status the account 2FA status
     */
    public abstract void set2FA(final boolean status);

    /**
     * Get the account id
     *
     * @return the account id
     */
    public abstract AccountID getUUID();

    /**
     * Get the account name
     *
     * @return the account name
     */
    public abstract String getName();

    /**
     * Save the account name
     *
     * @param name the account name
     */
    public abstract void setName(final String name);

    /**
     * Save the account password
     *
     * @param password the account password
     */
    public abstract void setPassword(final String password);

    /**
     * Save the account password unsafely
     *
     * @param password the account password
     */
    public abstract void setUnsafePassword(final String password);

    /**
     * Get the account password
     *
     * @return the account password
     */
    public abstract String getPassword();

    /**
     * Get if the account is registered
     *
     * @return if the account is registered
     */
    public abstract boolean isRegistered();

    /**
     * Save the account google auth token
     *
     * @param token the account google auth
     *              token
     */
    public abstract void setGAuth(final String token);

    /**
     * Save the account unsafe google auth token
     *
     * @param token the account google auth token
     */
    public abstract void setUnsafeGAuth(final String token);

    /**
     * Get the account google auth token
     *
     * @return the account google auth
     * token
     */
    public abstract String getGAuth();

    /**
     * Save the account pin
     *
     * @param pin the account pin
     */
    public abstract void setPin(final String pin);

    /**
     * Save the account unsafe pin
     *
     * @param pin the account pin
     */
    public abstract void setUnsafePin(final String pin);

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    public abstract String getPin();

    /**
     * Get if the account has pin
     *
     * @return if the account has pin
     */
    public abstract boolean hasPin();

    /**
     * Check if the account has 2FA
     *
     * @return if the account has 2FA
     */
    public abstract boolean has2FA();

    /**
     * Get the account creation time
     *
     * @return the account created time
     */
    public abstract Instant getCreationTime();

    /**
     * Get a list of accounts
     *
     * @return a list of all the
     * available accounts
     */
    public abstract Set<AccountManager> getAccounts();
}
