package ml.karmaconfigs.locklogin.plugin.common.session;

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
