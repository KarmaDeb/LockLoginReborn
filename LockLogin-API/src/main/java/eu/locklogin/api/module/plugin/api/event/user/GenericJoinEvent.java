package eu.locklogin.api.module.plugin.api.event.user;

import eu.locklogin.api.module.plugin.api.event.util.Event;

public abstract class GenericJoinEvent extends Event {

    private boolean handled = false;
    private String handleReason = "";

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
    public final void setHandled(final boolean status, final String reason) {
        handled = status;
        handleReason = reason;
    }
}
