package eu.locklogin.api.module.plugin.javamodule.server;

import com.google.gson.JsonObject;

/**
 * Message queue for server
 */
public abstract class MessageQue {

    /**
     * Add a message to the queue
     *
     * @param message the message to send
     */
    public abstract void add(final JsonObject message);

    /**
     * Read the next message in the queue, READING THE MESSAGE WILL
     * RESULT IN IT BEING REMOVED FROM THE QUEUE, PLEASE CALL THIS
     * METHOD CAREFULLY. To preview the next message run the method
     * {@link MessageQue#previewMessage()} instead
     *
     * @return the message to send
     */
    public abstract JsonObject nextMessage();

    /**
     * Preview the next message to prepare needed operations under it
     *
     * @return the next message
     */
    public abstract JsonObject previewMessage();
}
