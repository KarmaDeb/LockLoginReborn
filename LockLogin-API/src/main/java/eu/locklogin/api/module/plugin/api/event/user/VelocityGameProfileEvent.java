package eu.locklogin.api.module.plugin.api.event.user;

import eu.locklogin.api.module.plugin.api.event.util.Event;

/**
 * This event is fired when velocity requests a
 * game profile
 */
public class VelocityGameProfileEvent extends Event {

    private final Object eventObj;

    /**
     * Initialize the velocity game profile event
     *
     * @param event the velocity event
     */
    public VelocityGameProfileEvent(final Object event) {
        eventObj = event;
    }

    /**
     * Get if the event is handleable or not
     *
     * @return if the event is handleable
     */
    @Override
    public boolean isHandleable() {
        return false;
    }

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    @Override
    public boolean isHandled() {
        return false;
    }

    /**
     * Get the reason of why the event has been
     * marked as handled
     *
     * @return the event handle reason
     */
    @Override
    public String getHandleReason() {
        return "";
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     * @param reason the handle reason
     */
    @Override
    public void setHandled(boolean status, String reason) {
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public Object getEvent() {
        return eventObj;
    }
}
