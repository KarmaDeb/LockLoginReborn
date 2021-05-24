package ml.karmaconfigs.locklogin.api.modules.api.event.user;

import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.modules.util.client.ModulePlayer;
import ml.karmaconfigs.locklogin.api.modules.api.event.util.Event;

public final class SessionInitializationEvent extends Event {

    private final ModulePlayer modulePlayer;
    private final ClientSession session;
    private final Object event;
    private boolean handled = false;

    /**
     * Initialize the session initialization event
     *
     * @param _modulePlayer  the player who is being initialized
     * @param _session the player session
     * @param _event   the event owner
     */
    public SessionInitializationEvent(final ModulePlayer _modulePlayer, final ClientSession _session, final Object _event) {
        modulePlayer = _modulePlayer;
        session = _session;
        event = _event;
    }

    /**
     * Get the event player
     *
     * @return the event player
     */
    public final ModulePlayer getPlayer() {
        return modulePlayer;
    }

    /**
     * Get the player session
     *
     * @return the player session
     */
    public final ClientSession getSession() {
        return session;
    }

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    @Override
    public boolean isHandled() {
        return handled;
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     */
    @Override
    public void setHandled(boolean status) {
        handled = status;
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public Object getEvent() {
        return event;
    }
}
