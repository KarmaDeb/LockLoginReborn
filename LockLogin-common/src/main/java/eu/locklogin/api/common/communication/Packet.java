package eu.locklogin.api.common.communication;

import java.util.UUID;

/**
 * Packet implementation for LockLogin communication, doesn't really use
 * any RFC, is just a way to me to identify and avoid repeating messages
 */
public abstract class Packet {

    /**
     * Get the packet ID
     *
     * @return the packet ID
     */
    public abstract UUID packetID();

    /**
     * Get the packet data
     *
     * @return the packet data
     */
    public abstract byte[] packetData();

    /**
     * Get the packet raw data
     *
     * @return the packet raw data
     */
    public abstract String raw();
}
