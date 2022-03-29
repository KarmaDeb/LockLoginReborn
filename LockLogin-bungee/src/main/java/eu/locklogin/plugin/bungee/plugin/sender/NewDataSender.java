package eu.locklogin.plugin.bungee.plugin.sender;

import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.server.TargetServer;
import eu.locklogin.api.util.communication.Packet;
import eu.locklogin.api.util.communication.PluginMessenger;
import eu.locklogin.api.util.communication.handler.PacketAdapter;
import eu.locklogin.api.util.communication.handler.PacketHandler;
import eu.locklogin.api.util.communication.handler.event.PacketReceiveEvent;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NewDataSender extends PluginMessenger {

    private final static Map<Integer, LateScheduler<Packet>> waiting = new ConcurrentHashMap<>();

    private static PacketHandler handler = new PacketAdapter() {
        /**
         * On packet receive listener
         *
         * @param event the event
         */
        @Override
        public void onPacketReceived(PacketReceiveEvent event) {
            Packet packet = event.getPacket();
            int ack = packet.getIdentifier();

            LateScheduler<Packet> result = waiting.getOrDefault(ack, null);
            if (result != null) {
                result.complete(packet);
            }
        }
    };

    /**
     * Initialize the new data sender
     */
    public NewDataSender() {
        PacketAdapter.register(handler);
    }

    /**
     * Send a message to all the servers
     * <p>
     * Please note this method will use a random player in
     * the target server as recipient. If the server has no
     * players on it, no data will be sent unless you are using
     * Karma's remote messaging method
     *
     * @param data the data to send
     * @return the target response
     */
    @Override
    public LateScheduler<Packet> sendMessage(Packet data) {
        return null;
    }

    /**
     * Send a message to a specific server
     * <p>
     * Please note this method will use a random player in
     * the target server as recipient. If the server has no
     * players on it, no data will be sent unless you are using
     * Karma's remote messaging method
     *
     * @param server the server to send data to
     * @param data   the data to send
     * @return the target response
     */
    @Override
    public LateScheduler<Packet> sendMessage(TargetServer server, Packet data) {
        return null;
    }

    /**
     * Send a message for a player
     *
     * @param player the player to send the data
     * @param data   the data to send
     * @return the target response
     */
    @Override
    public LateScheduler<Packet> sendMessage(ModulePlayer player, Packet data) {
        return null;
    }
}
