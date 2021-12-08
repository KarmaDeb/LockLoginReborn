package eu.locklogin.api.module.plugin.api.event.server;

import eu.locklogin.api.module.plugin.api.event.util.Event;

public class ServerSendMessageEvent extends Event {

    private String message;

    private boolean handled = false;
    private String handleReason = "";

    /**
     * Initialize the server receive message event
     *
     * @param msg the message
     */
    public ServerSendMessageEvent(final String msg) {
        message = msg;
    }

    /**
     * Get the message
     *
     * @return the message
     */
    public final String getMessage() {
        return message;
    }

    /**
     * Set the message
     *
     * @param msg the message
     */
    public final void setMessage(final String msg) {
        if (!handled)
            message = msg;
    }

    /**
     * Get if the event is handleable or not
     *
     * @return if the event is handleable
     */
    @Override
    public final boolean isHandleable() {
        return true;
    }

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    @Override
    public final boolean isHandled() {
        return isHandleable() && handled;
    }

    /**
     * Get the reason of why the event has been
     * marked as handled
     *
     * @return the event handle reason
     */
    @Override
    public final String getHandleReason() {
        return handleReason;
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     * @param reason the handle reason
     */
    @Override
    public final void setHandled(boolean status, String reason) {
        handled = status;
        handleReason = reason;
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public final Object getEvent() {
        return null;
    }
}
