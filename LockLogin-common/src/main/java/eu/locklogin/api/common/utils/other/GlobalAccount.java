package eu.locklogin.api.common.utils.other;

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import ml.karmaconfigs.api.common.utils.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * LockLogin global account
 */
public class GlobalAccount extends AccountManager {

    private String name, password, pin, gauth;
    private AccountID uuid;
    private boolean enableFA;
    private final Instant creation;

    /**
     * Initialize the LockLogin global account
     *
     * @param importFrom the account to import from
     */
    public GlobalAccount(final AccountManager importFrom) {
        name = importFrom.getName();
        password = importFrom.getPassword();
        pin = importFrom.getPin();
        gauth = importFrom.getGAuth();
        uuid = importFrom.getUUID();
        enableFA = importFrom.has2FA();
        creation = importFrom.getCreationTime();
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
    public boolean remove(final String issuer) {
        return false;
    }

    /**
     * Save the account id
     *
     * @param id the account id
     */
    @Override
    public void saveUUID(AccountID id) {
        uuid = id;
    }

    /**
     * Save the account 2FA status
     *
     * @param status the account 2FA status
     */
    @Override
    public void set2FA(boolean status) {
        enableFA = status;
    }

    /**
     * Import the values from the specified account manager
     *
     * @param account the account
     */
    @Override
    protected void importFrom(final AccountManager account) {
        if (exists()) {
            name = account.getName();
            uuid = account.getUUID();
            password = account.getPassword();
            gauth = account.getGAuth();
            pin = account.getPin();
            enableFA = account.has2FA();
        }
    }

    /**
     * Get the account id
     *
     * @return the account id
     */
    @Override
    public AccountID getUUID() {
        return uuid;
    }

    /**
     * Get the account name
     *
     * @return the account name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Save the account name
     *
     * @param name the account name
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the account password
     *
     * @return the account password
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Get if the account is registered
     *
     * @return if the account is registered
     */
    @Override
    public boolean isRegistered() {
        return !StringUtils.isNullOrEmpty(password);
    }

    /**
     * Save the account password
     *
     * @param password the account password
     */
    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Save the account password unsafely
     *
     * @param password the account password
     */
    @Override
    public void setUnsafePassword(String password) {
        this.password = password;
    }

    /**
     * Get the account google auth token
     *
     * @return the account google auth
     * token
     */
    @Override
    public String getGAuth() {
        return gauth;
    }

    /**
     * Save the account google auth token
     *
     * @param token the account google auth
     *              token
     */
    @Override
    public void setGAuth(String token) {
        gauth = token;
    }

    /**
     * Save the account unsafe google auth token
     *
     * @param token the account google auth token
     */
    @Override
    public void setUnsafeGAuth(String token) {
        gauth = token;
    }

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    @Override
    public String getPin() {
        return pin;
    }

    /**
     * Get if the account has pin
     *
     * @return if the account has pin
     */
    @Override
    public boolean hasPin() {
        return !StringUtils.isNullOrEmpty(pin);
    }

    /**
     * Save the account pin
     *
     * @param pin the account pin
     */
    @Override
    public void setPin(String pin) {
        this.pin = pin;
    }

    /**
     * Save the account unsafe pin
     *
     * @param pin the account pin
     */
    @Override
    public void setUnsafePin(String pin) {
        this.pin = pin;
    }

    /**
     * Check if the account has 2FA
     *
     * @return if the account has 2FA
     */
    @Override
    public boolean has2FA() {
        return enableFA;
    }

    /**
     * Get the account creation time
     *
     * @return the account created time
     */
    @Override
    public Instant getCreationTime() {
        return creation;
    }

    /**
     * Get a list of accounts
     *
     * @return a list of all the
     * available accounts
     */
    @Override
    public Set<AccountManager> getAccounts() {
        return new LinkedHashSet<>(Collections.singleton(this));
    }
}
