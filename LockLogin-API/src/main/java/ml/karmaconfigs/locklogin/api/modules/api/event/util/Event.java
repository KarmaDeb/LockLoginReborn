package ml.karmaconfigs.locklogin.api.modules.api.event.util;

/**
 * LockLogin event
 */
public abstract class Event {

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    public abstract boolean isHandled();

    /**
     * Set the event handle status
     *
     * @param status the handle status
     */
    public abstract void setHandled(final boolean status);

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    public abstract Object getEvent();
}
