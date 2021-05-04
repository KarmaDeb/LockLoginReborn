package ml.karmaconfigs.locklogin.api.account;

import java.io.Serializable;
import java.util.UUID;

/**
 * LockLogin account id
 */
public final class AccountID implements Serializable {

    private final String id;

    /**
     * Initialize the account id
     *
     * @param uniqueId the account uuid
     */
    protected AccountID(final UUID uniqueId) {
        id = uniqueId.toString();
    }

    /**
     * Initialize the account id
     *
     * @param trimmedUUID the account trimmed uuid
     */
    protected AccountID(final String trimmedUUID) {
        id = trimmedUUID;
    }

    /**
     * Get an account id object from an UUID
     *
     * @param uuid the uuid
     * @return a new account id object
     */
    public static AccountID fromUUID(final UUID uuid) {
        return new AccountID(uuid);
    }

    /**
     * Get an account id object from a trimmed UUID
     *
     * @param trimmedUUID the trimmed UUID
     * @return a new account id object
     */
    public static AccountID fromTrimmed(final String trimmedUUID) {
        return new AccountID(trimmedUUID);
    }

    /**
     * Get the account id
     *
     * @return the account id
     */
    public final String getId() {
        return id;
    }
}
