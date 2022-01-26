package eu.locklogin.plugin.bungee.plugin;

import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
/*import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.remote.messaging.Server;
import ml.karmaconfigs.remote.messaging.listener.RemoteListener;
import ml.karmaconfigs.remote.messaging.listener.RemoteMessagingListener;
import ml.karmaconfigs.remote.messaging.listener.event.server.ClientMessageEvent;
import ml.karmaconfigs.remote.messaging.remote.RemoteClient;
import ml.karmaconfigs.remote.messaging.worker.tcp.TCPServer;*/

@SuppressWarnings("UnstableApiUsage")
public final class ServerHandler /*implements RemoteMessagingListener*/ {

    //private final Server server;

    public ServerHandler() {
        ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();
        /*server = new TCPServer(proxy.messageAddress(), proxy.messagePort()).debug(false);

        server.start().whenComplete((rsl, error) -> {
            if (error == null) {
                if (rsl) {
                    console.send("Registered server handler, modules will be able to communicate", Level.INFO);
                    CurrentPlatform.setRemoteServer(server);

                    RemoteListener.register(this);
                } else {
                    console.send("Failed to setup LockLogin remote server handler, modules can't communicate!", Level.GRAVE);
                }
            } else {
                logger.scheduleLog(Level.GRAVE, error);
                logger.scheduleLog(Level.INFO, "Failed to setup remote server handler");
                console.send("Failed to setup LockLogin remote server handler, modules can't communicate!", Level.GRAVE);
            }
        });*/
    }

    /**
     * Message received handler
     *
     * @param e the event
     */
    public void onMessageReceived(final Object e) {
        /*RemoteClient client = e.getClient();

        try {
            ByteArrayDataInput input = ByteStreams.newDataInput(e.getMessage());

            String target = input.readUTF();
            String serialized = input.readUTF();

            CurrentPlatform.getServer().getServer(target);
        } catch (Throwable ignored) {}*/
    }

    /**
     * Close the server
     */
    public void close() {
        /*server.exportBans(plugin.getDataPath().resolve("bans.lldb"));
        server.close();*/
    }
}
