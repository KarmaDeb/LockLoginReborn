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

import eu.locklogin.api.account.param.AccountConstructor;
import ml.karmaconfigs.api.common.string.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

/**
 * LockLogin account manager
 * <p>
 * MUST HAVE AN EMPTY CONSTRUCTOR
 */
public abstract class AccountManager implements Serializable {

    private final Class<?> instance;

    /**
     * Initialize the account manager
     *
     * @param constructor the account manager
     *                    constructor
     */
    public AccountManager(final @Nullable AccountConstructor<?> constructor) {
        if (constructor != null) {
            instance = constructor.getType();
        } else {
            instance = AccountManager.class;
        }
    }

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
     * @throws SecurityException if unauthorized access
     */
    public abstract boolean remove(final @NotNull String issuer) throws SecurityException;

    /**
     * Save the account id
     *
     * @param id the account id
     * @throws SecurityException if unauthorized access
     */
    public abstract void saveUUID(final @NotNull AccountID id) throws SecurityException;

    /**
     * Save the account 2FA status
     *
     * @param status the account 2FA status
     * @throws SecurityException if unauthorized access
     */
    public abstract void set2FA(final boolean status) throws SecurityException;

    /**
     * Import the values from the specified account manager
     *
     * @param account the account
     * @throws SecurityException if unauthorized access
     */
    protected abstract void importFrom(final @NotNull AccountManager account) throws SecurityException;

    /**
     * Get the account id
     *
     * @return the account id
     * @throws SecurityException if unauthorized access
     */
    public abstract @NotNull AccountID getUUID() throws SecurityException;

    /**
     * Get the account name
     *
     * @return the account name
     * @throws SecurityException if unauthorized access
     */
    public abstract @NotNull String getName() throws SecurityException;

    /**
     * Save the account name
     *
     * @param name the account name
     * @throws SecurityException if unauthorized access
     */
    public abstract void setName(final @NotNull String name) throws SecurityException;

    /**
     * Save the account password unsafely
     *
     * @param password the account password
     * @throws SecurityException if unauthorized access
     */
    public abstract void setUnsafePassword(final @Nullable String password) throws SecurityException;

    /**
     * Get the account password
     *
     * @return the account password
     * @throws SecurityException if unauthorized access
     */
    public abstract @NotNull String getPassword() throws SecurityException;

    /**
     * Save the account password
     *
     * @param password the account password
     * @throws SecurityException if unauthorized access
     */
    public abstract void setPassword(final @Nullable String password) throws SecurityException;

    /**
     * Get if the account is registered
     *
     * @return if the account is registered
     * @throws SecurityException if unauthorized access
     */
    public abstract boolean isRegistered() throws SecurityException;

    /**
     * Save the account unsafe google auth token
     *
     * @param token the account google auth token
     * @throws SecurityException if unauthorized access
     */
    public abstract void setUnsafeGAuth(final @Nullable String token) throws SecurityException;

    /**
     * Get the account google auth token
     *
     * @return the account google auth
     * token
     * @throws SecurityException if unauthorized access
     */
    public abstract @NotNull String getGAuth() throws SecurityException;

    /**
     * Save the account google auth token
     *
     * @param token the account google auth
     *              token
     * @throws SecurityException if unauthorized access
     */
    public abstract void setGAuth(final @Nullable String token) throws SecurityException;

    /**
     * Save the account unsafe pin
     *
     * @param pin the account pin
     * @throws SecurityException if unauthorized access
     */
    public abstract void setUnsafePin(final @Nullable String pin) throws SecurityException;

    /**
     * Get the account pin
     *
     * @return the account pin
     * @throws SecurityException if unauthorized access
     */
    public abstract @NotNull String getPin() throws SecurityException;

    /**
     * Save the account pin
     *
     * @param pin the account pin
     * @throws SecurityException if unauthorized access
     */
    public abstract void setPin(final @Nullable String pin) throws SecurityException;

    /**
     * Save the account panic unsafe token
     *
     * @param token the account panic token
     * @throws SecurityException if unauthorized access
     */
    public abstract void setUnsafePanic(final @Nullable String token) throws SecurityException;

    /**
     * Get the account panic token
     *
     * @return the account panic token
     * @throws SecurityException if unauthorized access
     */
    public abstract @NotNull String getPanic() throws SecurityException;

    /**
     * Save the account panic token
     *
     * @param token the panic token
     * @throws SecurityException if unauthorized access
     */
    public abstract void setPanic(final @Nullable String token) throws SecurityException;

    /**
     * Get if the account has pin
     *
     * @return if the account has pin
     * @throws SecurityException if unauthorized access
     */
    public boolean hasPin() throws SecurityException {
        String pin = getPin();
        return !StringUtils.isNullOrEmpty(pin);
    }

    /**
     * Check if the account has 2FA
     *
     * @return if the account has 2FA
     * @throws SecurityException if unauthorized access
     */
    public abstract boolean has2FA() throws SecurityException;

    /**
     * Get if the account has panic token
     *
     * @return if the account has panic token
     * @throws SecurityException if unauthorized access
     */
    public boolean hasPanic() throws SecurityException {
        String panic = getPanic();
        return !StringUtils.isNullOrEmpty(panic);
    }

    /**
     * Get the account creation time
     *
     * @return the account created time
     * @throws SecurityException if unauthorized access
     */
    public abstract @NotNull Instant getCreationTime() throws SecurityException;

    /**
     * Get a list of accounts
     *
     * @return a set of all the
     *         available accounts
     * @throws SecurityException if unauthorized access
     */
    public abstract @NotNull Set<AccountManager> getAccounts() throws SecurityException;

    /**
     * Get the manager constructor instance
     *
     * @return the manager constructor instance
     */
    @SuppressWarnings("unused")
    public final @NotNull Class<?> getOwnerConstructor() {
        return instance;
    }
}
