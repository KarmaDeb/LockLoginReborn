package ml.karmaconfigs.locklogin.api.modules.client;

public class MessageSender {

    private final Player target;
    private final String message;

    /**
     * Initialize the message sender
     *
     * @param tar the target
     * @param msg the message
     */
    public MessageSender(final Player tar, final String msg) {
        target = tar;
        message = msg;
    }

    /**
     * Get the message target
     *
     * @return the message player
     */
    public final Player getPlayer() {
        return target;
    }

    /**
     * Get the message
     *
     * @return the message
     */
    public final String getMessage() {
        return message;
    }
}
