package ml.karmaconfigs.locklogin.api.modules.event.user;

import ml.karmaconfigs.locklogin.api.modules.client.Player;
import ml.karmaconfigs.locklogin.api.modules.event.util.Event;

/**
 * This event is fired when a player joins
 * the server at the eyes of the plugin.
 */
public final class UserPostJoinEvent extends Event {

    private final Player player;
    private final Object eventObj;

    private boolean handled = false;

    /**
     * Initialize event
     *
     * @param player the player
     * @param event  the event in where this event is fired
     */
    public UserPostJoinEvent(final Player player, final Object event) {
        this.player = player;
        eventObj = event;
    }

    /**
     * Get the event player
     *
     * @return the event player
     */
    public final Player getPlayer() {
        return player;
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
     * Set the event handle status
     *
     * @param status the handle status
     */
    @Override
    public final void setHandled(boolean status) {
        handled = status;
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


