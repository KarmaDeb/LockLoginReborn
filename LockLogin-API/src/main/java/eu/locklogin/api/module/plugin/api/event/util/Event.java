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
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    public abstract boolean isHandled();

    /**
     * Set the event handle status
     *
     * @param status the handle status
     */
    public abstract void setHandled(final boolean status);

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    public abstract Object getEvent();
}
