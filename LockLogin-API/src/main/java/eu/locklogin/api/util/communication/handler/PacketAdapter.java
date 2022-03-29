package eu.locklogin.api.util.communication.handler;

import eu.locklogin.api.util.communication.handler.event.PacketEvent;
import eu.locklogin.api.util.communication.handler.event.PacketReceiveEvent;
import eu.locklogin.api.util.communication.handler.event.PacketSentEvent;

import java.util.Set;

/**
 * Simple LockLogin packet handler adapter
 */
public abstract class PacketAdapter implements PacketHandler {

    private static Set<PacketHandler> handlers;

    /**
     * On packet send listener
     *
     * @param event the event
     */
    @Override
    public void onPacketSent(PacketSentEvent event) {}

    /**
     * On packet receive listener
     *
     * @param event the event
     */
    @Override
    public void onPacketReceived(PacketReceiveEvent event) {}

    /**
     * Register a new packet handler
     *
     * @param handler the handler to register
     */
    public static void register(final PacketHandler handler) {
        handlers.add(handler);
    }

    /**
     * Unregister a packet handler
     *
     * @param handler the handler to unregister
     */
    public static void unregister(final PacketHandler handler) {
        handlers.remove(handler);
    }

    /**
     * Call an event
     *
     * @param event the event to call
     */
    public static void callEvent(final PacketEvent event) {
        handlers.forEach((handler) -> {
            if (event instanceof PacketSentEvent)
                handler.onPacketSent((PacketSentEvent) event);

            if (event instanceof PacketReceiveEvent)
                handler.onPacketReceived((PacketReceiveEvent) event);
        });
    }
}
