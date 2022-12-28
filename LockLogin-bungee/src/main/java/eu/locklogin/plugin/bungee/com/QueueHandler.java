package eu.locklogin.plugin.bungee.com;

import eu.locklogin.api.common.communication.queue.DataQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class QueueHandler {

    final static Map<String, DataQueue> server_queues = new ConcurrentHashMap<>();

}
