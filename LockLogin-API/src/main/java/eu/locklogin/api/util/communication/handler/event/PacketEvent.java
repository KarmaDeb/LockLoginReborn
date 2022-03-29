package eu.locklogin.api.util.communication.handler.event;

import eu.locklogin.api.util.communication.Packet;

/**
 * LockLogin generic packet event
 */
public abstract class PacketEvent {

    private final Packet packet;

    /**
     * Initialize the packet event
     *
     * @param pk the packet
     */
    public PacketEvent(final Packet pk) {
        //We clone it to make sure anything or none
        //can modify it. The integrity of the packet
        //is now secured ( partially )
        packet = pk.clone();
    }

    /**
     * Cancel the packet event
     *
     * @param status the event status
     */
    public abstract void setCancelled(final boolean status);

    /**
     * Get if the event is cancelled
     *
     * @return if the event is cancelled
     */
    public abstract boolean isCancelled();

    /**
     * Get the packet that is being sent
     *
     * @return the packet that is being sent
     */
    public final Packet getPacket() {
        //We don't want anything to be able to modify this
        return packet;
    }
}
