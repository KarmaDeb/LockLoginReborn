package eu.locklogin.api.security.backup.data;

import eu.locklogin.api.account.AccountID;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/**
 * Account backup
 */
public interface AccountBackup {

    /**
     * Get the account id
     *
     * @return the account id
     */
    AccountID id();

    /**
     * Get the account name
     *
     * @return the account name
     */
    String name();

    /**
     * Get the account password
     *
     * @return the account password
     */
    String password();

    /**
     * Get if the account has a password
     *
     * @return if the account has a password
     */
    boolean hasPassword();

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    String pin();

    /**
     * Get if the account has a pin
     *
     * @return if the account has ap in
     */
    boolean hasPin();

    /**
     * Get the account google auth token
     *
     * @return the account 2fa token
     */
    String googleAuthToken();

    /**
     * Get if the account has 2fa
     *
     * @return if the account has 2fa
     */
    boolean has2fa();

    /**
     * Get the account panic token
     *
     * @return the account panic token
     */
    String panic();

    /**
     * Get if the account has a panic token
     *
     * @return if the account has a panic token
     */
    boolean hasPanicToken();

    /**
     * Get the account creation time
     *
     * @return the account creation time
     */
    Instant creation();

    /**
     * Get the account locker backup
     *
     * @return the locked account or null if
     * not locked
     */
    @Nullable
    LockAccountBackup locker();
}
