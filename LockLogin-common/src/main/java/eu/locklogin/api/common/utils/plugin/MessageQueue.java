package eu.locklogin.api.common.utils.plugin;

import com.google.common.annotations.Beta;
import eu.locklogin.api.module.plugin.javamodule.server.MessageQue;
import eu.locklogin.api.module.plugin.javamodule.server.TargetServer;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * LockLogin message queue
 *
 * @beta This has not been tested yet, in fact, is not even
 * implemented in the plugin (only in the API). I will make
 * some tests before releasing the new module messaging API
 */
@Beta
public final class MessageQueue extends MessageQue {

    private final Map<TargetServer, Queue<byte[]>> queues = new ConcurrentHashMap<>();

    private final TargetServer server;

    /**
     * Initialize the message queue
     *
     * @param sv the server
     */
    public MessageQueue(final TargetServer sv) {
        server = sv;
    }

    /**
     * Add a message to the queue
     *
     * @param message the message to send
     */
    @Override
    public void add(final byte[] message) {
        Queue<byte[]> current_queue = queues.getOrDefault(server, new LinkedBlockingQueue<>());
        current_queue.add(message);
        queues.put(server, current_queue);
    }

    /**
     * Read the next message in the queue, READING THE MESSAGE WILL
     * RESULT IN IT BEING REMOVED FROM THE QUEUE, PLEASE CALL THIS
     * METHOD CAREFULLY. To preview the next message run the method
     * {@link MessageQue#previewMessage()} instead
     *
     * @return the message to send
     */
    @Override
    public byte[] nextMessage() {
        Queue<byte[]> current_queue = queues.getOrDefault(server, new LinkedBlockingQueue<>());
        return current_queue.poll();
    }

    /**
     * Preview the next message to prepare needed operations under it
     *
     * @return the next message
     */
    @Override
    public byte[] previewMessage() {
        Queue<byte[]> current_queue = queues.getOrDefault(server, new LinkedBlockingQueue<>());
        return current_queue.peek();
    }
}
