package ml.karmaconfigs.locklogin.api.modules.api.event.user;

import ml.karmaconfigs.locklogin.api.modules.util.client.ModulePlayer;
import ml.karmaconfigs.locklogin.api.modules.api.event.util.Event;

/**
 * This event is fired when a user quits
 * the server at the eyes of the plugin.
 * <p>
 * This means, this event will be fire when
 * a player quits the server, or it's kicked...
 */
public final class UserQuitEvent extends Event {

    private final ModulePlayer modulePlayer;
    private final Object eventObj;
    private boolean handled = false;

    /**
     * Initialize the event
     *
     * @param modulePlayerObject the player object
     * @param event        the event in where this event is fired
     */
    public UserQuitEvent(final ModulePlayer modulePlayerObject, final Object event) {
        modulePlayer = modulePlayerObject;
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
    public final ModulePlayer getPlayer() {
        return modulePlayer;
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
