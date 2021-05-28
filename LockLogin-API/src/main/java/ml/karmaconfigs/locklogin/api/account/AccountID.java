package ml.karmaconfigs.locklogin.api.account;

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
