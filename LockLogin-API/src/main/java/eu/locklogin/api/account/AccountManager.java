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
     */
    public abstract boolean remove(final @NotNull String issuer);

    /**
     * Save the account id
     *
     * @param id the account id
     */
    public abstract void saveUUID(final @NotNull AccountID id);

    /**
     * Save the account 2FA status
     *
     * @param status the account 2FA status
     */
    public abstract void set2FA(final boolean status);

    /**
     * Import the values from the specified account manager
     *
     * @param account the account
     */
    protected abstract void importFrom(final @NotNull AccountManager account);

    /**
     * Get the account id
     *
     * @return the account id
     */
    public abstract @NotNull AccountID getUUID();

    /**
     * Get the account name
     *
     * @return the account name
     */
    public abstract @NotNull String getName();

    /**
     * Save the account name
     *
     * @param name the account name
     */
    public abstract void setName(final @NotNull String name);

    /**
     * Save the account password unsafely
     *
     * @param password the account password
     */
    public abstract void setUnsafePassword(final @Nullable String password);

    /**
     * Get the account password
     *
     * @return the account password
     */
    public abstract @NotNull String getPassword();

    /**
     * Save the account password
     *
     * @param password the account password
     */
    public abstract void setPassword(final @Nullable String password);

    /**
     * Get if the account is registered
     *
     * @return if the account is registered
     */
    public abstract boolean isRegistered();

    /**
     * Save the account unsafe google auth token
     *
     * @param token the account google auth token
     */
    public abstract void setUnsafeGAuth(final @Nullable String token);

    /**
     * Get the account google auth token
     *
     * @return the account google auth
     * token
     */
    public abstract @NotNull String getGAuth();

    /**
     * Save the account google auth token
     *
     * @param token the account google auth
     *              token
     */
    public abstract void setGAuth(final @Nullable String token);

    /**
     * Save the account unsafe pin
     *
     * @param pin the account pin
     */
    public abstract void setUnsafePin(final @Nullable String pin);

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    public abstract @NotNull String getPin();

    /**
     * Save the account pin
     *
     * @param pin the account pin
     */
    public abstract void setPin(final @Nullable String pin);

    /**
     * Save the account panic unsafe token
     *
     * @param token the account panic token
     */
    public abstract void setUnsafePanic(final @Nullable String token);

    /**
     * Get the account panic token
     *
     * @return the account panic token
     */
    public abstract @NotNull String getPanic();

    /**
     * Save the account panic token
     *
     * @param token the panic token
     */
    public abstract void setPanic(final @Nullable String token);

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
    public abstract @NotNull Instant getCreationTime();

    /**
     * Get a list of accounts
     *
     * @return a list of all the
     * available accounts
     */
    public abstract @NotNull Set<AccountManager> getAccounts();

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
