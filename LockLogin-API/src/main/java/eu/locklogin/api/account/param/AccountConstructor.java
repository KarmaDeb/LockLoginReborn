package eu.locklogin.api.account.param;

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

import eu.locklogin.api.account.AccountManager;
import org.jetbrains.annotations.Nullable;

/**
 * The account constructor parameter is a required
 * parameter of {@link AccountManager} required for
 * the plugin to initialize it
 *
 * @param <T> the account constructor type
 */
public abstract class AccountConstructor<T> {

    /**
     * Get the parameter of the account parameter
     *
     * @return the account constructor parameter
     */
    @Nullable
    public abstract Parameter<T> getParameter();

    /**
     * Get a class instance of the account constructor
     * type
     *
     * @return the account constructor type
     */
    @Nullable
    public abstract Class<? extends T> getType();
}
