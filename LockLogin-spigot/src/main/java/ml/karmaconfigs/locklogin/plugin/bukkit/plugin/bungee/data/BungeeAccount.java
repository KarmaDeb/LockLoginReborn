package ml.karmaconfigs.locklogin.plugin.bukkit.plugin.bungee.data;

import ml.karmaconfigs.locklogin.api.account.AccountID;
import ml.karmaconfigs.locklogin.api.account.AccountManager;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

public class BungeeAccount extends AccountManager {

    private final Instant created;
    private AccountID accountID;
    private String accountName;
    private String accountPassword;
    private String accountPin;
    private String accountToken;
    private boolean account2FA;

    /**
     * Initialize the bungeecord account
     *
     * @param name     the bungee account name
     * @param id       the bungee account id
     * @param password the bungee account password
     * @param pin      the bungee account pin
     * @param token    the bungee account google auth token
     * @param gAuth    the bungee account google auth status
     * @param date     the bungee account creation date
     */
    public BungeeAccount(final String name, final AccountID id, final String password, final String pin, final String token, final boolean gAuth, final Instant date) {
        accountID = id;
        accountName = name;
        accountPassword = password;
        accountPin = pin;
        accountToken = token;
        account2FA = gAuth;
        created = date;
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
     * @return if the account could be removed
     */
    @Override
    public boolean remove() {
        return false;
    }

    /**
     * Save the account id
     *
     * @param id the account id
     */
    @Override
    public void saveUUID(AccountID id) {
        accountID = id;
    }

    /**
     * Save the account 2FA status
     *
     * @param status the account 2FA status
     */
    @Override
    public void set2FA(boolean status) {
        account2FA = status;
    }

    /**
     * Get the account id
     *
     * @return the account id
     */
    @Override
    public AccountID getUUID() {
        return accountID;
    }

    /**
     * Get the account name
     *
     * @return the account name
     */
    @Override
    public String getName() {
        return accountName;
    }

    /**
     * Save the account name
     *
     * @param name the account name
     */
    @Override
    public void setName(String name) {
        accountName = name;
    }

    /**
     * Get the account password
     *
     * @return the account password
     */
    @Override
    public String getPassword() {
        return accountPassword;
    }

    /**
     * Save the account password
     *
     * @param password the account password
     */
    @Override
    public void setPassword(String password) {
        accountPassword = password;
    }

    /**
     * Get the account google auth token
     *
     * @return the account google auth
     * token
     */
    @Override
    public String getGAuth() {
        return accountToken;
    }

    /**
     * Save the account google auth token
     *
     * @param token the account google auth
     *              token
     */
    @Override
    public void setGAuth(String token) {
        accountToken = token;
    }

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    @Override
    public String getPin() {
        return accountPin;
    }

    /**
     * Save the account pin
     *
     * @param pin the account pin
     */
    @Override
    public void setPin(String pin) {
        accountPin = pin;
    }

    /**
     * Check if the account has 2FA
     *
     * @return if the account has 2FA
     */
    @Override
    public boolean has2FA() {
        return account2FA;
    }

    /**
     * Get the account creation time
     *
     * @return the account created time
     */
    @Override
    public Instant getCreationTime() {
        return created;
    }

    /**
     * Get a list of accounts
     *
     * @return a list of all the
     * available accounts
     */
    @Override
    public Set<AccountManager> getAccounts() {
        return Collections.emptySet();
    }
}
