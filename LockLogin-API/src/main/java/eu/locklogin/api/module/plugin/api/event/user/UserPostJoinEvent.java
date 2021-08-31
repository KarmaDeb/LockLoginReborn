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

import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;

/**
 * This event is fired when a player joins
 * the server at the eyes of the plugin.
 */
public final class UserPostJoinEvent extends GenericJoinEvent {

    private final ModulePlayer modulePlayer;
    private final Object eventObj;

    private boolean handled = false;
    private String handleReason = "";

    /**
     * Initialize event
     *
     * @param modulePlayer the player
     * @param event  the event in where this event is fired
     */
    public UserPostJoinEvent(final ModulePlayer modulePlayer, final Object event) {
        this.modulePlayer = modulePlayer;
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
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public Object getEvent() {
        return eventObj;
    }
}


