package ml.karmaconfigs.locklogin.api.modules.api.event.user;

import ml.karmaconfigs.locklogin.api.modules.util.client.ModulePlayer;
import ml.karmaconfigs.locklogin.api.modules.api.event.util.Event;
import org.jetbrains.annotations.Nullable;

public final class AccountCreatedEvent extends Event {

    private final ModulePlayer modulePlayer;
    private final Object event;
    private boolean handled = false;

    /**
     * Initialize the account creation event
     *
     * @param _modulePlayer the player who created the account
     * @param _event  the event owner
     */
    public AccountCreatedEvent(final ModulePlayer _modulePlayer, final Object _event) {
        modulePlayer = _modulePlayer;
        event = _event;
    }

    /**
     * Get the player who created the account
     *
     * @return the player who created
     * the account
     */
    public final ModulePlayer getPlayer() {
        return modulePlayer;
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
    @Nullable
    public Object getEvent() {
        return event;
    }
}
