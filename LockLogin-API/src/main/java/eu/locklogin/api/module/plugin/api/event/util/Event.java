package eu.locklogin.api.module.plugin.api.event.util;

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

/**
 * LockLogin event
 */
public abstract class Event {

    /**
     * Get if the event is handleable or not
     *
     * @return if the event is handleable
     */
    public abstract boolean isHandleable();

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    public abstract boolean isHandled();

    /**
     * Get the reason of why the event has been
     * marked as handled
     *
     * @return the event handle reason
     */
    public abstract String getHandleReason();

    /**
     * Set the event handle status
     *
     * @param status the handle status
     * @param reason the handle reason
     */
    public abstract void setHandled(final boolean status, final String reason);

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    public abstract Object getEvent();
}
