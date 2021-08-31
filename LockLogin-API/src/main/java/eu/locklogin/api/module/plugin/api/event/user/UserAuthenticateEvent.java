package eu.locklogin.api.module.plugin.api.event.user;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;

/**
 * This event is fired when an user auths.
 */
public final class UserAuthenticateEvent extends Event {

    private final AuthType auth_type;
    private final ModulePlayer modulePlayer;
    private final Result auth_result;
    private final String auth_message;
    private final Object eventObj;

    private boolean handled = false;
    private String handleReason = "";

    /**
     * Initialize the player auth event
     *
     * @param _auth_type    the auth type
     * @param _auth_result  the auth result
     * @param _modulePlayer       the player
     * @param _auth_message the auth message
     * @param event         the event in where this event is fired
     */
    public UserAuthenticateEvent(final AuthType _auth_type, final Result _auth_result, final ModulePlayer _modulePlayer, final String _auth_message, final Object event) {
        auth_type = _auth_type;
        auth_result = _auth_result;
        modulePlayer = _modulePlayer;
        auth_message = _auth_message;

        eventObj = event;
    }

    /**
     * Get the event player
     *
     * @return the event player
     */
    public ModulePlayer getPlayer() {
        return modulePlayer;
    }

    /**
     * Get the auth type
     *
     * @return the auth type
     */
    public AuthType getAuthType() {
        return auth_type;
    }

    /**
     * Get the auth result
     *
     * @return the auth result
     */
    public Result getAuthResult() {
        return auth_result;
    }

    /**
     * Get the auth message
     *
     * @return the auth message
     */
    public String getAuthMessage() {
        return auth_message;
    }

    /**
     * Get if the event is handleable or not
     *
     * @return if the event is handleable
     */
    @Override
    public boolean isHandleable() {
        return false;
    }

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    @Override
    public boolean isHandled() {
        return isHandleable() && handled;
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
        return eventObj;
    }

    /**
     * LockLogin available auth types
     */
    public enum AuthType {
        /**
         * LockLogin valid auth type
         */
        PASSWORD,
        /**
         * LockLogin valid auth type
         */
        PIN,
        /**
         * LockLogin valid auth type
         */
        FA_2,
        /**
         * LockLogin valid auth type
         */
        API
    }

    /**
     * LockLogin valid auth results
     * for this event
     */
    public enum Result {
        /**
         * LockLogin auth event result waiting for validation
         */
        WAITING,
        /**
         * LockLogin auth event result failed validation
         */
        FAILED,
        /**
         * LockLogin auth event result validation success
         */
        SUCCESS,
        /**
         * LockLogin auth event result validation success but has 2fa or pin
         */
        SUCCESS_TEMP,
        /**
         * LockLogin auth event result something went wrong
         */
        ERROR
    }
}
