package eu.locklogin.api.common.communication;

import eu.locklogin.api.common.communication.queue.DataQueue;

/**
 * Data sender standard for LockLogin
 */
public abstract class DataSender<T> {

    /**
     * Get the data queue
     *
     * @param name the queue name
     * @return the data queue
     */
    public abstract DataQueue queue(final T name);
}
