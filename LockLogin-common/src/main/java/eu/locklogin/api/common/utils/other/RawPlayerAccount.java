package eu.locklogin.api.common.utils.other;

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import ml.karmaconfigs.api.common.string.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
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
@SuppressWarnings("unused")
public class RawPlayerAccount extends AccountManager {

    private AccountID id;
    private String name;
    private String password;
    private String pin;
    private String token;
    private String panic;
    private boolean gAuth;
    private long creation;

    RawPlayerAccount() {
        super(null);
    }

    public static AccountManager fromPlayerAccount(final AccountManager manager) {
        RawPlayerAccount raw = new RawPlayerAccount();
        raw.id = manager.getUUID();
        raw.name = manager.getName();
        raw.password = manager.getPassword();
        raw.pin = manager.getPin();
        raw.token = manager.getGAuth();
        raw.panic = manager.getPanic();
        raw.gAuth = manager.has2FA();
        raw.creation = manager.getCreationTime().toEpochMilli();

        return raw;
    }

    /**
     * Check if the file exists
     *
     * @return if the file exists
     */
    @Override
    public boolean exists() {
        return true;
    }

    /**
     * Tries to create the account
     *
     * @return if the account could be created
     */
    @Override
    public boolean create() {
        return false;
    }

    /**
     * Tries to remove the account
     *
     * @param issuer the account removal issuer
     * @return if the account could be removed
     * @throws SecurityException if unauthorized access
     */
    @Override
    public boolean remove(@NotNull String issuer) throws SecurityException {
        return false;
    }

    /**
     * Save the account id
     *
     * @param id the account id
     * @throws SecurityException if unauthorized access
     */
    @Override
    public void saveUUID(@NotNull AccountID id) throws SecurityException {

    }

    /**
     * Save the account 2FA status
     *
     * @param status the account 2FA status
     * @throws SecurityException if unauthorized access
     */
    @Override
    public void set2FA(boolean status) throws SecurityException {

    }

    /**
     * Import the values from the specified account manager
     *
     * @param account the account
     * @throws SecurityException if unauthorized access
     */
    @Override
    protected void importFrom(@NotNull AccountManager account) throws SecurityException {

    }

    /**
     * Get the account id
     *
     * @return the account id
     * @throws SecurityException if unauthorized access
     */
    @Override
    public @NotNull AccountID getUUID() throws SecurityException {
        return id;
    }

    /**
     * Get the account name
     *
     * @return the account name
     * @throws SecurityException if unauthorized access
     */
    @Override
    public @NotNull String getName() throws SecurityException {
        return name;
    }

    /**
     * Save the account name
     *
     * @param name the account name
     * @throws SecurityException if unauthorized access
     */
    @Override
    public void setName(@NotNull String name) throws SecurityException {

    }

    /**
     * Save the account password unsafely
     *
     * @param password the account password
     * @throws SecurityException if unauthorized access
     */
    @Override
    public void setUnsafePassword(@Nullable String password) throws SecurityException {

    }

    /**
     * Get the account password
     *
     * @return the account password
     * @throws SecurityException if unauthorized access
     */
    @Override
    public @NotNull String getPassword() throws SecurityException {
        return password;
    }

    /**
     * Save the account password
     *
     * @param password the account password
     * @throws SecurityException if unauthorized access
     */
    @Override
    public void setPassword(@Nullable String password) throws SecurityException {

    }

    /**
     * Get if the account is registered
     *
     * @return if the account is registered
     * @throws SecurityException if unauthorized access
     */
    @Override
    public boolean isRegistered() throws SecurityException {
        return !StringUtils.isNullOrEmpty(password);
    }

    /**
     * Save the account unsafe google auth token
     *
     * @param token the account google auth token
     * @throws SecurityException if unauthorized access
     */
    @Override
    public void setUnsafeGAuth(@Nullable String token) throws SecurityException {

    }

    /**
     * Get the account google auth token
     *
     * @return the account google auth
     * token
     * @throws SecurityException if unauthorized access
     */
    @Override
    public @NotNull String getGAuth() throws SecurityException {
        return token;
    }

    /**
     * Save the account google auth token
     *
     * @param token the account google auth
     *              token
     * @throws SecurityException if unauthorized access
     */
    @Override
    public void setGAuth(@Nullable String token) throws SecurityException {

    }

    /**
     * Save the account unsafe pin
     *
     * @param pin the account pin
     * @throws SecurityException if unauthorized access
     */
    @Override
    public void setUnsafePin(@Nullable String pin) throws SecurityException {

    }

    /**
     * Get the account pin
     *
     * @return the account pin
     * @throws SecurityException if unauthorized access
     */
    @Override
    public @NotNull String getPin() throws SecurityException {
        return pin;
    }

    /**
     * Save the account pin
     *
     * @param pin the account pin
     * @throws SecurityException if unauthorized access
     */
    @Override
    public void setPin(@Nullable String pin) throws SecurityException {

    }

    /**
     * Save the account panic unsafe token
     *
     * @param token the account panic token
     * @throws SecurityException if unauthorized access
     */
    @Override
    public void setUnsafePanic(@Nullable String token) throws SecurityException {

    }

    /**
     * Get the account panic token
     *
     * @return the account panic token
     * @throws SecurityException if unauthorized access
     */
    @Override
    public @NotNull String getPanic() throws SecurityException {
        return panic;
    }

    /**
     * Save the account panic token
     *
     * @param token the panic token
     * @throws SecurityException if unauthorized access
     */
    @Override
    public void setPanic(@Nullable String token) throws SecurityException {

    }

    /**
     * Check if the account has 2FA
     *
     * @return if the account has 2FA
     * @throws SecurityException if unauthorized access
     */
    @Override
    public boolean has2FA() throws SecurityException {
        return gAuth;
    }

    /**
     * Get the account creation time
     *
     * @return the account created time
     * @throws SecurityException if unauthorized access
     */
    @Override
    public @NotNull Instant getCreationTime() throws SecurityException {
        return Instant.ofEpochMilli(creation);
    }

    /**
     * Get a list of accounts
     *
     * @return a set of all the
     * available accounts
     * @throws SecurityException if unauthorized access
     */
    @Override
    public @NotNull Set<AccountManager> getAccounts() throws SecurityException {
        return new HashSet<>();
    }
}
