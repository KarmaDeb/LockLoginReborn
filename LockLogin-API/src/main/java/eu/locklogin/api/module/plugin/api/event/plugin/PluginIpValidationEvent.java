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

import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.net.InetAddress;

/**
 * This event is fired when an IP is validated by LockLogin
 * <p>
 * This event is fired before {@link eu.locklogin.api.module.plugin.api.event.user.UserPreJoinEvent},
 * {@link eu.locklogin.api.module.plugin.api.event.user.UserJoinEvent} and {@link eu.locklogin.api.module.plugin.api.event.user.UserPostJoinEvent}
 */
public final class PluginIpValidationEvent extends Event {

    private final InetAddress address;
    private final ValidationProcess process;
    private final Object owner;

    private ValidationResult validationResult;
    private PluginModule handleOwner;

    private boolean handled = false;
    private String handleReason = "";

    /**
     * Initialize the plugin ip validation event
     *
     * @param ip     the ip
     * @param pro    the process
     * @param res    the result
     * @param reason the result reason
     * @param event  the event owner
     */
    public PluginIpValidationEvent(final InetAddress ip, final ValidationProcess pro, final ValidationResult res, final String reason, final Object event) {
        address = ip;
        process = pro;
        validationResult = res.withReason(reason);
        owner = event;
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
    public void setHandled(final boolean status, final @NotNull String reason) {
        handled = status;
        handleReason = reason;
        validationResult = ValidationResult.INVALID.withReason("Event handled");
    }

    /**
     * Set the event handle status
     *
     * @param issuer the handle issuer
     * @param result the validation result
     * @param status the handle status
     * @param reason the handle reason
     */
    public void setHandled(final PluginModule issuer, final ValidationResult result, final boolean status, final @NotNull String reason) {
        handleOwner = issuer;
        if (ModuleLoader.isLoaded(issuer)) {
            validationResult = result;
            handled = status;
            handleReason = reason;
        }
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public Object getEvent() {
        return owner;
    }

    /**
     * Get the handle owner
     *
     * @return the handle owner
     */
    @Nullable
    public PluginModule getHandleOwner() {
        return handleOwner;
    }

    /**
     * Get the ip that has been checked
     *
     * @return the ip that has been checked
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Get the validation process
     *
     * @return the validation process
     */
    public ValidationProcess getProcess() {
        return process;
    }

    /**
     * Get the validation result
     *
     * @return the validation result
     */
    public ValidationResult getResult() {
        return validationResult;
    }

    /**
     * Ip validation process
     */
    public static enum ValidationProcess {
        /**
         * Server ping process
         */
        SERVER_PING,
        /**
         * Pre join process
         */
        VALID_IP,
        /**
         * Post join process
         */
        PROXY_IP
    }

    /**
     * Ip validation result
     */
    public static enum ValidationResult {
        /**
         * Validation is success ( IP is valid )
         */
        SUCCESS("UNKNOWN"),
        /**
         * Validation got error ( The IP is invalid because the check could not be done )
         */
        ERROR("UNKNOWN"),
        /**
         * Validation got invalid ( The plugin determined the IP is invalid )
         */
        INVALID("UNKNOWN");

        String reason;

        ValidationResult(final String rsn) {
            reason = rsn;
        }

        /**
         * Update the reason
         *
         * @param rsn the reason
         */
        public final ValidationResult withReason(final String rsn) {
            reason = rsn;

            return this;
        }

        /**
         * Get the reason
         *
         * @return the reason
         */
        public final String getReason() {
            return reason;
        }
    }
}
