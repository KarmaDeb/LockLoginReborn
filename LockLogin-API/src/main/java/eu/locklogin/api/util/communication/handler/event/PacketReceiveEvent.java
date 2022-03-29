package eu.locklogin.api.util.communication.handler.event;

import eu.locklogin.api.util.communication.Packet;

/**
 * Initialize the packet sent event
 */
public class PacketReceiveEvent extends PacketEvent {

    /**
     * Initialize the packet event
     *
     * @param pk the packet
     */
    public PacketReceiveEvent(Packet pk) {
        super(pk);
    }

    /**
     * Cancel the packet event
     *
     * @param status the event status
     */
    @Override
    public void setCancelled(boolean status) {

    }

    /**
     * Get if the event is cancelled
     *
     * @return if the event is cancelled
     */
    @Override
    public boolean isCancelled() {
        return false;
    }
}
