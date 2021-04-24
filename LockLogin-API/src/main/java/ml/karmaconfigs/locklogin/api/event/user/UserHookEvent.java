package ml.karmaconfigs.locklogin.api.event.user;

import ml.karmaconfigs.locklogin.api.event.util.Event;
import org.jetbrains.annotations.Nullable;

/**
 * This event is fire when a player is hook
 * after a plugin reload or if the plugin has
 * been loaded by a third party plugin loader.
 *
 * This event is also fired on the first plugin start
 */
public final  class UserHookEvent extends Event {

    private boolean handled = false;

    private final Object player;
    private final Object eventObj;

    /**
     * Initialize the event
     *
     * @param playerObject the player object
     * @param event the event in where this event is fired
     */
    public UserHookEvent(final Object playerObject, final Object event) {
        player = playerObject;
        eventObj = event;
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     */
    @Override
    public final void setHandled(boolean status) {
        handled = true;
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
     * Get the player
     *
     * @return the player
     */
    public final Object getPlayer() {
        return player;
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public final @Nullable Object getEvent() {
        return eventObj;
    }
}
