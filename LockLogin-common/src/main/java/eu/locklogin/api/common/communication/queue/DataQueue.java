package eu.locklogin.api.common.communication.queue;

import eu.locklogin.api.common.communication.Packet;
import org.jetbrains.annotations.ApiStatus;

/**
 * Data queue
 */
public abstract class DataQueue {

    /**
     * Insert data into the queue
     *
     * @param data the data to insert
     * @deprecated The new method that uses packet should be used instead
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "1.13.40")
    @Deprecated
    public void insert(final byte[] data) {
        insert(data, false);
    }

    /**
     * Insert packet into the queue
     *
     * @param packet the packet to insert
     */
    public void insert(final Packet packet) {
        insert(packet, false);
    }

    /**
     * Insert the data
     *
     * @param data the data to insert
     * @param onTop force the data to be on top, so it will be the next one
     * @deprecated The new method that uses packet should be used instead
     */
    @ApiStatus.ScheduledForRemoval(inVersion = "1.13.40")
    @Deprecated
    public abstract void insert(final byte[] data, final boolean onTop);

    /**
     * Insert the packet
     *
     * @param packet the packet to insert
     * @param onTop force the packet to be on top, so it will be the next packet
     *              when retrieving
     */
    public abstract void insert(final Packet packet, final boolean onTop);

    /**
     * Get the next entry
     *
     * @return the next entry
     */
    public abstract Packet next();

    /**
     * Shift the second element to top and the top element
     * to the end of the queue
     */
    public abstract void shift();

    /**
     * Consume the next entry
     */
    public abstract void consume();

    /**
     * Cancel the next data read
     */
    public abstract void cancel();

    /**
     * Unlock the queue, once this is unlocked,
     * it won't be locked again
     */
    public abstract void unlock();

    /**
     * Get if the queue is locked
     *
     * @return if the queue is locker
     */
    public abstract boolean locked();
}
