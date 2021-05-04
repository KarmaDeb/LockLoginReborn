package ml.karmaconfigs.locklogin.api.modules.event.user;

import ml.karmaconfigs.locklogin.api.modules.client.Player;
import ml.karmaconfigs.locklogin.api.modules.event.util.Event;
import org.jetbrains.annotations.Nullable;

/**
 * This event is fired when the plugin
 * unhooks a player, this event can happen
 * when the server is stopped, or when the plugin
 * is about to be updated.
 */
public final class UserUnHookEvent extends Event {

    private final Player player;
    private final Object eventObj;
    private boolean handled = false;

    /**
     * Initialize event
     *
     * @param playerObject the player
     * @param event        the event in where this event is fired
     */
    public UserUnHookEvent(final Player playerObject, final Object event) {
        player = playerObject;
        eventObj = event;
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
        handled = true;
    }

    /**
     * Get the player
     *
     * @return the player
     */
    public final Player getPlayer() {
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
