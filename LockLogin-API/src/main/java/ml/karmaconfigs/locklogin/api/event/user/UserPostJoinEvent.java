package ml.karmaconfigs.locklogin.api.event.user;

import ml.karmaconfigs.locklogin.api.event.util.Event;

/**
 * This event is fired when a player joins
 * the server at the eyes of the plugin.
 */
public final class UserPostJoinEvent extends Event {

    private final Object player;
    private final Object eventObj;

    private boolean handled = false;

    /**
     * Initialize event
     *
     * @param player the player
     * @param event the event in where this event is fired
     */
    public UserPostJoinEvent(final Object player, final Object event) {
        this.player = player;
        eventObj = event;
    }

    /**
     * Get the event player
     *
     * @return the event player
     */
    public final Object getPlayer() {
        return player;
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     */
    @Override
    public final void setHandled(boolean status) {
        handled = status;
    }

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    @Override
    public final boolean isHandled() {
        return handled;
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public final Object getEvent() {
        return eventObj;
    }
}


