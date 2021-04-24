package ml.karmaconfigs.locklogin.api.event.plugin;

import ml.karmaconfigs.locklogin.api.event.util.Event;
import org.jetbrains.annotations.Nullable;

/**
 * This event is fired when a player
 * or the console types migrate command, with
 * an invalid argument, for example /locklogin migrate Hello.
 *
 * So if a module has a custom migration method, it can be
 * handled by it using this event.
 */
public final class MigrationRequestEvent extends Event {

    private boolean handled = false;

    private final String arg;
    private final String[] parameters;
    private final Object commandSender;
    private final Object eventObj;

    /**
     * Initialize event
     *
     * @param argument the command argument
     * @param params the command parameters
     * @param sender the command sender
     * @param event the event in where this event is fired
     */
    public MigrationRequestEvent(final String argument, final String[] params, final Object sender, final Object event) {
        arg = argument;
        parameters = params;
        commandSender = sender;
        eventObj = event;
    }

    /**
     * Get the command event argument
     *
     * @return the argument
     */
    public final String getArgument() {
        return arg;
    }

    /**
     * Get the command event parameters
     *
     * @return the command parameters
     */
    public final String[] getParameters() {
        return parameters;
    }

    /**
     * Get the command sender
     *
     * @return the command sender
     */
    public final Object getSender() {
        return commandSender;
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     */
    @Override
    public final void setHandled(boolean status) {
        handled = status;
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
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public final @Nullable Object getEvent() {
        return null;
    }
}
