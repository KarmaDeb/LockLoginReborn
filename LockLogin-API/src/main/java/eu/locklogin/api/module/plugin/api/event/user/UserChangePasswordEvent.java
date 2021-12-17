package eu.locklogin.api.module.plugin.api.event.user;

import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;

/**
 * This event is fired when a player tries to
 * change his password
 */
public final class UserChangePasswordEvent extends Event {

    private final ModulePlayer player;
    private final ChangeResult result;

    private boolean handled = false;
    private String handleReason = "";

    /**
     * Initialize event
     *
     * @param issuer the player that changed his password
     * @param rs the event main result
     */
    public UserChangePasswordEvent(final ModulePlayer issuer, final ChangeResult rs) {
        player = issuer;
        result = rs;
    }

    /**
     * Get the player
     *
     * @return the player
     */
    public ModulePlayer getPlayer() {
        return player;
    }

    /**
     * Get if the player was originally able to
     * change his password
     *
     * @return if the player was originally able
     * to change his password
     */
    public ChangeResult getResult() {
        return result;
    }

    /**
     * Get if the event is handleable or not
     *
     * @return if the event is handleable
     */
    @Override
    public boolean isHandleable() {
        return true;
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
     * Get the reason of why the event has been
     * marked as handled
     *
     * @return the event handle reason
     */
    @Override
    public String getHandleReason() {
        return handleReason;
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     * @param reason the handle reason
     */
    @Override
    public void setHandled(final boolean status, final String reason) {
        handled = status;
        handleReason = reason;
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public Object getEvent() {
        return null;
    }

    /**
     * LockLogin available change password
     * results
     */
    public enum ChangeResult {
        /**
         * LockLogin valid change password result
         */
        ALLOWED,
        /**
         * LockLogin valid change password result
         */
        ALLOWED_UNSAFE,
        /**
         * LockLogin valid change password result
         */
        DENIED_SAME,
        /**
         * LockLogin valid change password result
         */
        DENIED_UNSAFE
    }
}
