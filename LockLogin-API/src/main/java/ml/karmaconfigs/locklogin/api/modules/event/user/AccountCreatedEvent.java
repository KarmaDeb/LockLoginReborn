package ml.karmaconfigs.locklogin.api.modules.event.user;

import ml.karmaconfigs.locklogin.api.modules.client.Player;
import ml.karmaconfigs.locklogin.api.modules.event.util.Event;
import org.jetbrains.annotations.Nullable;

public final class AccountCreatedEvent extends Event {

    private final Player player;
    private final Object event;
    private boolean handled = false;

    /**
     * Initialize the account creation event
     *
     * @param _player the player who created the account
     * @param _event  the event owner
     */
    public AccountCreatedEvent(final Player _player, final Object _event) {
        player = _player;
        event = _event;
    }

    /**
     * Get the player who created the account
     *
     * @return the player who created
     * the account
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
