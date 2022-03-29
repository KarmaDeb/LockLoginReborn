package eu.locklogin.api.util.communication;

import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.module.plugin.javamodule.server.TargetServer;
import ml.karmaconfigs.api.common.timer.scheduler.LateScheduler;

/**
 * LockLogin plugin messenger
 */
public abstract class PluginMessenger {

    /**
     * Send a message to all the servers
     *
     * Please note this method will use a random player in
     * the target server as recipient. If the server has no
     * players on it, no data will be sent unless you are using
     * Karma's remote messaging method
     *
     * @param data the data to send
     * @return the target response
     */
    public abstract LateScheduler<Packet> sendMessage(final Packet data);

    /**
     * Send a message to a specific server
     *
     * Please note this method will use a random player in
     * the target server as recipient. If the server has no
     * players on it, no data will be sent unless you are using
     * Karma's remote messaging method
     *
     * @param server the server to send data to
     * @param data the data to send
     * @return the target response
     */
    public abstract LateScheduler<Packet> sendMessage(final TargetServer server, final Packet data);

    /**
     * Send a message for a player
     *
     * @param player the player to send the data
     * @param data the data to send
     * @return the target response
     */
    public abstract LateScheduler<Packet> sendMessage(final ModulePlayer player, final Packet data);
}
