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

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.module.plugin.api.event.util.Event;

import java.net.InetAddress;
import java.util.UUID;

/**
 * This event is fired when a player joins
 * the server at the eyes of the plugin.
 */
public final class UserPreJoinEvent extends Event {

    private final InetAddress address;
    private final UUID id;
    private final String player;
    private final Object eventObject;

    private boolean handled = false;
    private String handleReason = "";

    /**
     * Initialize event
     *
     * @param ip    the player ip
     * @param uuid  the player uuid
     * @param name  the player name
     * @param event the event instance
     */
    public UserPreJoinEvent(final InetAddress ip, final UUID uuid, final String name, final Object event) {
        address = ip;
        id = uuid;
        player = name;
        eventObject = event;
    }

    /**
     * Get the player ip
     *
     * @return the player ip
     */
    public InetAddress getIp() {
        return address;
    }

    /**
     * Get the player account id
     *
     * @return the player account id
     */
    public AccountID getAccountId() {
        return AccountID.fromUUID(id);
    }

    /**
     * Get the event player
     *
     * @return the event player
     */
    public String getName() {
        return player;
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
        return eventObject;
    }
}

