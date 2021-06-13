package eu.locklogin.api.common.session;

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

/**
 * LockLogin logged/registered data
 */
public final class SessionChangeData {

    private final DataType type;
    private final DataChange change;
    private final int amount;

    /**
     * Initialize the session change data
     *
     * @param _type      the change type
     * @param _change    the data change type
     * @param changeSize the number change amount
     */
    public SessionChangeData(final DataType _type, final DataChange _change, final int changeSize) {
        type = _type;
        change = _change;
        amount = changeSize;
    }

    /**
     * Get the data type
     *
     * @return the data type
     */
    public final DataType getDataType() {
        return type;
    }

    /**
     * Get the data change type
     *
     * @return the change type
     */
    public final DataChange getChangeType() {
        return change;
    }

    /**
     * Get the change size
     *
     * @return the changed size
     */
    public final int getSize() {
        return amount;
    }

    public enum DataType {
        LOGIN, REGISTER;
    }

    public enum DataChange {
        DECREASE, INCREASE, SAME
    }
}
