package ml.karmaconfigs.locklogin.plugin.common.utils;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

/**
 * LockLogin plugin message pool
 */
public class MessagePool {

    private final Object server;
    private final Object message;

    /**
     * Initialize the message pool
     *
     * @param target the message target server
     * @param data the plugin message data
     */
    public MessagePool(final Object target, final Object data) {
        server = target;
        message = data;
    }

    /**
     * Get the message target server
     *
     * @return the message target server
     */
    public final Object getServer() {
        return server;
    }

    /**
     * Get the plugin message
     *
     * @return the plugin message
     */
    public final Object getMessage() {
        return message;
    }
}
