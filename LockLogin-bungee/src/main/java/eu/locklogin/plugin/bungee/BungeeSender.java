package eu.locklogin.plugin.bungee;

import eu.locklogin.api.common.communication.DataSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import org.jetbrains.annotations.Nullable;

public final class BungeeSender {

    public static DataSender sender;
    public static boolean useSocket = false;

    public static int registered_servers = 0;

    public static String serverFromPlayer(final @Nullable ProxiedPlayer player) {
        ServerInfo info = ProxyServer.getInstance().getServers().values().toArray(new ServerInfo[0])[0]; //Get first server, which won't be always the correct one
        if (player != null) {
            Server connected = player.getServer();
            if (connected != null && connected.getInfo() != null) {
                info = connected.getInfo();
            }
        }

        return info.getName();
    }
}
