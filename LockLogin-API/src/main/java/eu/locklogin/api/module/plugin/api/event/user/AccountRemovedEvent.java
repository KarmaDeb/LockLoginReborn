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

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import org.jetbrains.annotations.Nullable;

/**
 * This event is fired when a new account is removed
 */
public final class AccountRemovedEvent extends Event {

    private final AccountManager removedAccount;
    private final String issuer;
    private final Object event;

    private boolean handled = false;
    private String handleReason = "";

    /**
     * Initialize the account creation event
     *
     * @param _removedAccount the account that got removed
     * @param _issuer         the account removal issuer
     * @param _event          the event owner
     */
    public AccountRemovedEvent(final AccountManager _removedAccount, final String _issuer, final Object _event) {
        removedAccount = _removedAccount;
        issuer = _issuer;
        event = _event;
    }

    /**
     * Get the player who created the account
     *
     * @return the player who created
     * the account
     */
    public AccountManager getRemovedAccount() {
        return removedAccount;
    }

    /**
     * Get the account removal issuer
     *
     * @return the account remove issuer
     */
    public String getIssuer() {
        return issuer;
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
    @Nullable
    public Object getEvent() {
        return event;
    }
}
