package eu.locklogin.plugin.bungee.util.tmp;

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.param.AccountConstructor;
import eu.locklogin.api.account.param.Parameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Set;

public class TemporalFixedAccount extends AccountManager {

    private final TemporalAccountWrappedData data;

    /**
     * Initialize the account manager
     *
     * @param constructor the account manager
     *                    constructor
     * @throws IllegalArgumentException if the constructor is not complete
     */
    public TemporalFixedAccount(@Nullable AccountConstructor<? extends TemporalAccountWrappedData> constructor) throws IllegalArgumentException {
        super(constructor);
        if (constructor == null || constructor.getParameter() == null) {
            throw new IllegalArgumentException("Missing temporal account data for temporal fixed account");
        }

        data = constructor.getParameter().getValue();
    }

    /**
     * Check if the file exists
     *
     * @return if the file exists
     */
    @Override
    public boolean exists() {
        return false;
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
     */
    @Override
    public boolean remove(@NotNull String issuer) {
        return false;
    }

    /**
     * Save the account id
     *
     * @param id the account id
     */
    @Override
    public void saveUUID(@NotNull AccountID id) {

    }

    /**
     * Save the account 2FA status
     *
     * @param status the account 2FA status
     */
    @Override
    public void set2FA(boolean status) {

    }

    /**
     * Import the values from the specified account manager
     *
     * @param account the account
     */
    @Override
    protected void importFrom(@NotNull AccountManager account) {

    }

    /**
     * Get the account id
     *
     * @return the account id
     */
    @Override
    public @NotNull AccountID getUUID() {
        return AccountID.fromString(data.getUuid());
    }

    /**
     * Get the account name
     *
     * @return the account name
     */
    @Override
    public @NotNull String getName() {
        return data.getName();
    }

    /**
     * Save the account name
     *
     * @param name the account name
     */
    @Override
    public void setName(@NotNull String name) {

    }

    /**
     * Save the account password unsafely
     *
     * @param password the account password
     */
    @Override
    public void setUnsafePassword(@Nullable String password) {

    }

    /**
     * Get the account password
     *
     * @return the account password
     */
    @Override
    public @NotNull String getPassword() {
        return data.getPassword();
    }

    /**
     * Save the account password
     *
     * @param password the account password
     */
    @Override
    public void setPassword(@Nullable String password) {

    }

    /**
     * Get if the account is registered
     *
     * @return if the account is registered
     */
    @Override
    public boolean isRegistered() {
        return false;
    }

    /**
     * Save the account unsafe google auth token
     *
     * @param token the account google auth token
     */
    @Override
    public void setUnsafeGAuth(@Nullable String token) {

    }

    /**
     * Get the account google auth token
     *
     * @return the account google auth
     * token
     */
    @Override
    public @NotNull String getGAuth() {
        return data.getToken();
    }

    /**
     * Save the account google auth token
     *
     * @param token the account google auth
     *              token
     */
    @Override
    public void setGAuth(@Nullable String token) {

    }

    /**
     * Save the account unsafe pin
     *
     * @param pin the account pin
     */
    @Override
    public void setUnsafePin(@Nullable String pin) {

    }

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    @Override
    public @NotNull String getPin() {
        return data.getPin();
    }

    /**
     * Save the account pin
     *
     * @param pin the account pin
     */
    @Override
    public void setPin(@Nullable String pin) {

    }

    /**
     * Save the account panic unsafe token
     *
     * @param token the account panic token
     */
    @Override
    public void setUnsafePanic(@Nullable String token) {

    }

    /**
     * Get the account panic token
     *
     * @return the account panic token
     */
    @Override
    public @NotNull String getPanic() {
        return data.getPanic();
    }

    /**
     * Save the account panic token
     *
     * @param token the panic token
     */
    @Override
    public void setPanic(@Nullable String token) {

    }

    /**
     * Get if the account has pin
     *
     * @return if the account has pin
     */
    @Override
    public boolean hasPin() {
        return false;
    }

    /**
     * Check if the account has 2FA
     *
     * @return if the account has 2FA
     */
    @Override
    public boolean has2FA() {
        return false;
    }

    /**
     * Get the account creation time
     *
     * @return the account created time
     */
    @Override
    public @NotNull Instant getCreationTime() {
        return null;
    }

    /**
     * Get a list of accounts
     *
     * @return a list of all the
     * available accounts
     */
    @Override
    public @NotNull Set<AccountManager> getAccounts() {
        return null;
    }
}
