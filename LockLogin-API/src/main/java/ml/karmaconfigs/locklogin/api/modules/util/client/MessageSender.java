package ml.karmaconfigs.locklogin.api.modules.util.client;

public class MessageSender {

    private final ModulePlayer target;
    private final String message;

    /**
     * Initialize the message sender
     *
     * @param tar the target
     * @param msg the message
     */
    public MessageSender(final ModulePlayer tar, final String msg) {
        target = tar;
        message = msg;
    }

    /**
     * Get the message target
     *
     * @return the message player
     */
    public final ModulePlayer getPlayer() {
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
