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
import eu.locklogin.api.account.param.Parameter;
import eu.locklogin.api.account.param.SimpleParameter;
import ml.karmaconfigs.api.bukkit.util.UUIDUtil;
import ml.karmaconfigs.api.common.utils.string.StringUtils;

import java.io.Serializable;
import java.util.UUID;

/**
 * LockLogin account id
 */
public final class AccountID extends AccountConstructor<AccountID> implements Serializable {

    private final String id;

    /**
     * Initialize the account id
     *
     * @param uniqueId the account uuid
     */
    private AccountID(final UUID uniqueId) {
        id = uniqueId.toString();
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
     * Get an account id object from a string UUID
     *
     * @param stringUUID the string UUID
     * @return a new account id object
     *
     * @throws IllegalArgumentException if the trimmed UUID is not a
     * valid trimmed UUID
     */
    public static AccountID fromString(final String stringUUID) throws IllegalArgumentException {
        UUID result = UUIDUtil.fromTrimmed(stringUUID);

        if (result != null) {
            return new AccountID(result);
        } else {
            throw new IllegalArgumentException("Cannot create account id of trimmed UUID for invalid string UUID ( " + stringUUID + " )");
        }
    }

    /**
     * Get the account id
     *
     * @return the account id
     */
    public String getId() {
        return id;
    }

    /**
     * Get the parameter of the account parameter
     *
     * @return the account constructor parameter
     */
    @Override
    public Parameter<AccountID> getParameter() {
        return new SimpleParameter<>("accountid", this);
    }

    /**
     * Get a class instance of the account constructor
     * type
     *
     * @return the account constructor type
     */
    @Override
    public Class<? extends AccountID> getType() {
        return AccountID.class;
    }
}
