package ml.karmaconfigs.locklogin.plugin.common.session;

import java.util.function.Consumer;

public final class SessionDataContainer {

    private static Consumer<SessionChangeData> onDataChange = null;

    private static int logged = 0;
    private static int registered = 0;

    /**
     * Set an action to perform when the data change
     *
     * @param onChange the new on change consumer
     */
    public static void onDataChange(final Consumer<SessionChangeData> onChange) {
        onDataChange = onChange;
    }

    /**
     * Get the logged users amount
     *
     * @return the logged users amount
     */
    public static int getLogged() {
        return logged;
    }

    /**
     * Set the logged users amount
     *
     * @param amount the logged users amount
     */
    public static void setLogged(final int amount) {
        int before = logged;
        logged = amount;

        if (onDataChange != null) {
            SessionChangeData.DataChange change = SessionChangeData.DataChange.SAME;
            int size = 0;

            if (before != logged) {
                change = (before > logged ? SessionChangeData.DataChange.DECREASE : SessionChangeData.DataChange.INCREASE);
                size = Math.abs(before - logged);
            }

            SessionChangeData data = new SessionChangeData(SessionChangeData.DataType.LOGIN, change, size);
            onDataChange.accept(data);
        }
    }

    /**
     * Get registered users amount
     *
     * @return the registered users amount
     */
    public static int getRegistered() {
        return registered;
    }

    /**
     * Set the registered users amount
     *
     * @param amount the registered users amount
     */
    public static void setRegistered(final int amount) {
        int before = registered;
        registered = amount;

        if (onDataChange != null) {
            SessionChangeData.DataChange change = SessionChangeData.DataChange.SAME;
            int size = 0;

            if (before != logged) {
                change = (before > logged ? SessionChangeData.DataChange.DECREASE : SessionChangeData.DataChange.INCREASE);
                size = Math.abs(before - logged);
            }

            SessionChangeData data = new SessionChangeData(SessionChangeData.DataType.REGISTER, change, size);
            onDataChange.accept(data);
        }
    }
}
