package eu.locklogin.api.common.security.backup.data;

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.security.backup.data.AccountBackup;
import eu.locklogin.api.security.backup.data.LockAccountBackup;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

@AllArgsConstructor
public class JsonBackupAccount implements AccountBackup {

    private final AccountID id;
    private final String name;
    private final String password;
    private final String pin;
    private final String token;
    private final String panic;
    private final long creation;
    private final LockAccountBackup lock;
    private final boolean isPass;
    private final boolean isPin;
    private final boolean is2FA;
    private final boolean isPanic;

    /**
     * Get the account id
     *
     * @return the account id
     */
    @Override
    public AccountID id() {
        return id;
    }

    /**
     * Get the account name
     *
     * @return the account name
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Get the account password
     *
     * @return the account password
     */
    @Override
    public String password() {
        return password;
    }

    /**
     * Get if the account has a password
     *
     * @return if the account has a password
     */
    @Override
    public boolean hasPassword() {
        return isPass;
    }

    /**
     * Get the account pin
     *
     * @return the account pin
     */
    @Override
    public String pin() {
        return pin;
    }

    /**
     * Get if the account has a pin
     *
     * @return if the account has ap in
     */
    @Override
    public boolean hasPin() {
        return isPin;
    }

    /**
     * Get the account google auth token
     *
     * @return the account 2fa token
     */
    @Override
    public String googleAuthToken() {
        return token;
    }

    /**
     * Get if the account has 2fa
     *
     * @return if the account has 2fa
     */
    @Override
    public boolean has2fa() {
        return is2FA;
    }

    /**
     * Get the account panic token
     *
     * @return the account panic token
     */
    @Override
    public String panic() {
        return panic;
    }

    /**
     * Get if the account has a panic token
     *
     * @return if the account has a panic token
     */
    @Override
    public boolean hasPanicToken() {
        return isPanic;
    }

    /**
     * Get the account creation time
     *
     * @return the account creation time
     */
    @Override
    public Instant creation() {
        return Instant.ofEpochMilli(creation);
    }

    /**
     * Get the account locker backup
     *
     * @return the locked account or null if
     * not locked
     */
    @Override
    public @Nullable LockAccountBackup locker() {
        return lock;
    }
}
