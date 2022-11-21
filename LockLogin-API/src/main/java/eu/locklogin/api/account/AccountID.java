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
import ml.karmaconfigs.api.common.utils.uuid.UUIDUtil;

import java.io.Serializable;
import java.util.HashMap;
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
     */
    public static AccountID fromString(final String stringUUID) {
        UUID result = UUIDUtil.fromTrimmed(stringUUID);
        assert result != null;

        return new AccountID(result);
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

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The {@code equals} method implements an equivalence relation
     * on non-null object references:
     * <ul>
     * <li>It is <i>reflexive</i>: for any non-null reference value
     *     {@code x}, {@code x.equals(x)} should return
     *     {@code true}.
     * <li>It is <i>symmetric</i>: for any non-null reference values
     *     {@code x} and {@code y}, {@code x.equals(y)}
     *     should return {@code true} if and only if
     *     {@code y.equals(x)} returns {@code true}.
     * <li>It is <i>transitive</i>: for any non-null reference values
     *     {@code x}, {@code y}, and {@code z}, if
     *     {@code x.equals(y)} returns {@code true} and
     *     {@code y.equals(z)} returns {@code true}, then
     *     {@code x.equals(z)} should return {@code true}.
     * <li>It is <i>consistent</i>: for any non-null reference values
     *     {@code x} and {@code y}, multiple invocations of
     *     {@code x.equals(y)} consistently return {@code true}
     *     or consistently return {@code false}, provided no
     *     information used in {@code equals} comparisons on the
     *     objects is modified.
     * <li>For any non-null reference value {@code x},
     *     {@code x.equals(null)} should return {@code false}.
     * </ul>
     * <p>
     * The {@code equals} method for class {@code Object} implements
     * the most discriminating possible equivalence relation on objects;
     * that is, for any non-null reference values {@code x} and
     * {@code y}, this method returns {@code true} if and only
     * if {@code x} and {@code y} refer to the same object
     * ({@code x == y} has the value {@code true}).
     * <p>
     * Note that it is generally necessary to override the {@code hashCode}
     * method whenever this method is overridden, so as to maintain the
     * general contract for the {@code hashCode} method, which states
     * that equal objects must have equal hash codes.
     *
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     * argument; {@code false} otherwise.
     * @see #hashCode()
     * @see HashMap
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof UUID) {
            return obj.equals(UUID.fromString(id));
        }
        if (obj instanceof String) {
            try {
                return obj.equals(id);
            } catch (Throwable ex) {
                return false;
            }
        }

        if (obj instanceof AccountID) {
            AccountID accountID = (AccountID) obj;
            return accountID.getId().equals(id);
        }

        return false;
    }
}
