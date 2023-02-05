package eu.locklogin.plugin.bungee.com;

import eu.locklogin.api.common.communication.queue.DataQueue;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class QueueHandler {

    final static Map<String, DataQueue> proxy_server_queues = new ConcurrentHashMap<>();
    final static Map<ServerInfo, DataQueue> bungee_server_queues = new ConcurrentHashMap<>();

}
