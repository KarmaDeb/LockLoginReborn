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

import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.ModulePlayer;

/**
 * This event is fired once per-player, and it's fired when
 * a player session is initialized by LockLogin
 */
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
