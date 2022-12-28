package eu.locklogin.plugin.bungee.com.queue;

import eu.locklogin.api.common.communication.Packet;
import eu.locklogin.api.common.communication.queue.DataQueue;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import static eu.locklogin.plugin.bungee.LockLogin.console;

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
        /*if (onTop) {
            queue.addFirst(data);
        } else {
            queue.addLast(data);
        }*/
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
            console.send("Inserting message");
            if (onTop) {
                queue.addFirst(packet);
            } else {
                queue.add(packet);
            }
        }
    }


    /**
     * Get the next entry
     *
     * @return the next entry
     */
    @Override
    public byte[] next() {
        if (!read && !locked) {
            read = true;
            Packet next = queue.peek();
            if (next != null)
                return next.packetData();
        }

        return null;
    }

    /**
     * Shift the second element to top and the top element
     * to the end of the queue
     */
    @Override
    public void shift() {
        if (!locked) {
            Packet top = queue.poll();
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
        if (!locked) {
            queue.poll();
            read = false;
        }
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
}
