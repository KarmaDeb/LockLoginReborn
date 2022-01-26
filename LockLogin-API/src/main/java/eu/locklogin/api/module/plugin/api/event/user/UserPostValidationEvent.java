package eu.locklogin.api.module.plugin.api.event.user;

import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;

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
 * This event is fired when a player is verified
 * by spigot
 */
public final class UserPostValidationEvent extends Event {

    private final Object event;
    private final ModulePlayer player;
    private final String server;

    /**
     * Initialize user post validation event
     *
     * @param client the client that has been validated
     * @param name the client name
     * @param owner the event owner
     */
    public UserPostValidationEvent(final ModulePlayer client, final String name, final Object owner) {
        player = client;
        server = name;
        event = owner;
    }

    /**
     * Get the player that has been validated
     *
     * @return the player that has been validated
     */
    public ModulePlayer getPlayer() {
        return player;
    }

    /**
     * Get the server the player has been validated in
     *
     * @return the server name
     */
    public String getServer() {
        return server;
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
        return false;
    }

    /**
     * Get the reason of why the event has been
     * marked as handled
     *
     * @return the event handle reason
     */
    @Override
    public String getHandleReason() {
        return "";
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     * @param reason the handle reason
     */
    @Override
    public void setHandled(boolean status, String reason) {}

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
