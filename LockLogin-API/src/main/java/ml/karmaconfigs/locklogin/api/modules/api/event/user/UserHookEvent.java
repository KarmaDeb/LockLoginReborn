package ml.karmaconfigs.locklogin.api.modules.api.event.user;

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

import ml.karmaconfigs.locklogin.api.modules.api.event.util.Event;
import ml.karmaconfigs.locklogin.api.modules.util.client.ModulePlayer;
import org.jetbrains.annotations.Nullable;

/**
 * This event is fire when a player is hook
 * after a plugin reload or if the plugin has
 * been loaded by a third party plugin loader.
 * <p>
 * This event is also fired on the first plugin start
 */
public final class UserHookEvent extends Event {

    private final ModulePlayer modulePlayer;
    private final Object eventObj;
    private boolean handled = false;

    /**
     * Initialize the event
     *
     * @param modulePlayerObject the player object
     * @param event        the event in where this event is fired
     */
    public UserHookEvent(final ModulePlayer modulePlayerObject, final Object event) {
        modulePlayer = modulePlayerObject;
        eventObj = event;
    }

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    @Override
    public final boolean isHandled() {
        return handled;
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     */
    @Override
    public final void setHandled(boolean status) {
        handled = true;
    }

    /**
     * Get the player
     *
     * @return the player
     */
    public final ModulePlayer getPlayer() {
        return modulePlayer;
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public final @Nullable Object getEvent() {
        return eventObj;
    }
}
