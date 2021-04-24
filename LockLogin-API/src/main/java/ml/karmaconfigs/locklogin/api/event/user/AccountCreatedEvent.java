package ml.karmaconfigs.locklogin.api.event.user;

import ml.karmaconfigs.locklogin.api.event.util.Event;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class AccountCreatedEvent extends Event {

    private boolean handled = false;

    private final Player player;
    private final Object event;

    /**
     * Initialize the account creation event
     *
     * @param _player the player who created the account
     * @param _event the event owner
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
     * Set the event handle status
     *
     * @param status the handle status
     */
    @Override
    public void setHandled(boolean status) {
        handled = status;
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
