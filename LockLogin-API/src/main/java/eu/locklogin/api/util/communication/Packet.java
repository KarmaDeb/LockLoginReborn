package eu.locklogin.api.util.communication;

import java.io.Serializable;

/**
 * LockLogin packet, following TCP
 * segment header format but very simplified.
 */
public abstract class Packet implements Serializable, Cloneable {

    /**
     * Get the packet source
     *
     * @return the packet source
     */
    public abstract String getSource();

    /**
     * Get the packet target
     *
     * @return the packet target
     */
    public abstract String getTarget();

    /**
     * Get the packet identifier
     *
     * @return the packet identifier
     */
    public abstract int getIdentifier();

    /**
     * Get the packet priority. This helps the plugin
     * to know which packet to read first and later
     *
     * @return the packet priority
     */
    public abstract int getPriority();

    /**
     * Get the data of the packet
     *
     * @return the packet data
     */
    public abstract byte[] getData();

    /**
     * Get the data checksum
     *
     * @return the data checksum, used to check
     * provided data has no errors. If it has, the
     * packet should be requested again
     */
    public abstract long getChecksum();

    /**
     * Clone the packet
     *
     * @return the clonned packet
     */
    @Override
    public Packet clone() {
        try {
            return (Packet) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
