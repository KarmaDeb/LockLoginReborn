package ml.karmaconfigs.locklogin.api.event.plugin;

import ml.karmaconfigs.locklogin.api.event.util.Event;
import org.jetbrains.annotations.Nullable;

public final class UpdateRequestEvent extends Event {

    private boolean handled = false;
    private final Object event;

    private final Object commandSender;
    private final boolean isUnsafe;

    /**
     * Initialize the update request event
     *
     * @param sender the update request sender
     * @param unsafe if the sender has unsafe update permission
     * @param eventOwner the update request owner
     */
    public UpdateRequestEvent(final Object sender, final boolean unsafe, final Object eventOwner) {
        commandSender = sender;
        isUnsafe = unsafe;
        event = eventOwner;
    }

    /**
     * Get the sender who requested update
     *
     * @return the update request issuer
     */
    public final Object getSender() {
        return commandSender;
    }

    /**
     * Get if the sender who requested update
     * has unsafe update permission
     *
     * @return if the update issuer has unsafe
     * update permission
     */
    public final boolean canPerformUnsafeUpdate() {
        return isUnsafe;
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
