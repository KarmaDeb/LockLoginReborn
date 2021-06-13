package eu.locklogin.api.module.plugin.api.event.plugin;

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
import org.jetbrains.annotations.Nullable;

/**
 * This event is fired when the plugin requests to
 * update itself, this even is fired from LockLoginManager
 * module
 */
public final class UpdateRequestEvent extends Event {

    private final Object event;
    private final Object commandSender;
    private final boolean isUnsafe;
    private boolean handled = false;

    /**
     * Initialize the update request event
     *
     * @param sender     the update request sender
     * @param unsafe     if the sender has unsafe update permission
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
