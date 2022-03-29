package eu.locklogin.api.util.communication.handler;

import eu.locklogin.api.util.communication.handler.event.PacketReceiveEvent;
import eu.locklogin.api.util.communication.handler.event.PacketSentEvent;

/**
 * Simple LockLogin packet handler
 */
@SuppressWarnings("unused")
public interface PacketHandler {

    /**
     * On packet send listener
     *
     * @param event the event
     */
    void onPacketSent(final PacketSentEvent event);

    /**
     * On packet receive listener
     *
     * @param event the event
     */
    void onPacketReceived(final PacketReceiveEvent event);
}
