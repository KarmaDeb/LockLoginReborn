package eu.locklogin.plugin.bungee;

import eu.locklogin.api.common.communication.DataSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BungeeSender {

    public DataSender sender; //For security reasons, this should never be static
    public DataSender secondarySender; //For security reasons, this should never be static
    public static boolean useSocket = false;

    public static int registered_servers = 0;

    private final static Set<String> force_bungee = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void forceBungee(final ServerInfo server) {
        force_bungee.add(server.getName());
    }

    public static void useProxy(final ServerInfo server) {
        force_bungee.remove(server.getName());
    }

    public static boolean isForceBungee(final ServerInfo server) {
        if (server == null)
            return false;

        return force_bungee.contains(server.getName());
    }

    public static ServerInfo serverFromPlayer(final @Nullable ProxiedPlayer player) {
        ServerInfo info = ProxyServer.getInstance().getServers().values().toArray(new ServerInfo[0])[0]; //Get first server, which won't be always the correct one
        if (player != null) {
            Server connected = player.getServer();
            if (connected != null && connected.getInfo() != null) {
                info = connected.getInfo();
            }
        }

        return info;
    }
}
