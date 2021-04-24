package ml.karmaconfigs.locklogin.api.event.util;

/**
 * LockLogin event
 */
public abstract class Event {

    /**
     * Set the event handle status
     *
     * @param status the handle status
     */
    public abstract void setHandled(final boolean status);

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    public abstract boolean isHandled();

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    public abstract Object getEvent();
}
