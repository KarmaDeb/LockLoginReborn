package eu.locklogin.api.common.web.alert;

/**
 * Notification for {@link RemoteNotification}
 */
public final class Notification {

    private final int level;
    private final String notification;

    private final boolean config;
    private final boolean proxy;

    /**
     * Initialize the notification
     *
     * @param lvl the notification level
     * @param text the notification text
     * @param cfg force the cfg values
     * @param px force the proxy values
     */
    public Notification(final int lvl, final String text, final boolean cfg, final boolean px) {
        level = lvl;
        notification = text;
        config = cfg;
        proxy = px;
    }

    /**
     * Get the notification level
     *
     * @return the notification level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Get the notification text
     *
     * @return the notification information
     */
    public String getNotification() {
        return notification;
    }

    /**
     * Get if the current status forces plugin configuration
     *
     * @return if the current status forces plugin configuration
     */
    public boolean forceConfig() {
        return config;
    }

    /**
     * Get if the current status forces proxy settings
     *
     * @return if the current status forces proxy settings
     */
    public boolean forceProxy() {
        return proxy;
    }
}
