package eu.locklogin.plugin.bungee.com.queue;

import eu.locklogin.api.common.communication.Packet;
import eu.locklogin.api.common.communication.queue.DataQueue;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Proxy message queue
 */
public class MessageQueue extends DataQueue {

    private final Deque<Packet> queue = new ConcurrentLinkedDeque<>();
    private boolean read = false;
    private boolean locked = true;

    /**
     * Insert the data
     *
     * @param data  the data to insert
     * @param onTop force the data to be on top, so it will be the next one
     */
    @Override
    public void insert(final byte[] data, final boolean onTop) {
        throw new RuntimeException("Cannot insert non-packet data to the queue!");
    }

    /**
     * Insert the packet
     *
     * @param packet the packet to insert
     * @param onTop  force the packet to be on top, so it will be the next packet
     *               when retrieving
     */
    @Override
    public void insert(final Packet packet, final boolean onTop) {
        if (queue.stream().noneMatch((p) -> p.packetID().equals(packet.packetID()))) {
            if (onTop) {
                queue.addFirst(packet);
            } else {
                queue.addLast(packet);
            }
        }
    }


    /**
     * Get the next entry
     *
     * @return the next entry
     */
    @Override
    public Packet next() {
        if (!read) {
            read = true;
            return queue.peek();
        }

        return null;
    }

    /**
     * Shift the second element to top and the top element
     * to the end of the queue
     */
    @Override
    public void shift() {
        Packet top = queue.poll();
        if (top != null) {
            queue.add(top);
            if (read)
                read = false;
        }
    }

    /**
     * Consume the next entry
     */
    @Override
    public void consume() {
        if (read) {
            queue.poll();
        }

        read = false;
    }

    /**
     * Cancel the next data read
     */
    @Override
    public void cancel() {
        read = false;
    }

    /**
     * Unlock the queue, once this is unlocked,
     * it won't be locked again
     */
    @Override
    public void unlock() {
        locked = false;
    }

    /**
     * Get if the queue is locked
     *
     * @return if the queue is locker
     */
    @Override
    public boolean locked() {
        return locked;
    }

    public int size() {
        return queue.size();
    }
}
