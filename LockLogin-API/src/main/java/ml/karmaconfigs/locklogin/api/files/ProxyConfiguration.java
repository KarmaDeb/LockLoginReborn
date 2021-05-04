package ml.karmaconfigs.locklogin.api.files;

import java.util.Iterator;
import java.util.UUID;

public abstract class ProxyConfiguration {

    /**
     * Get if the proxy has multiple bungeecord
     * instances
     *
     * @return if the server has multiple bungeecord instances
     */
    public abstract boolean multiBungee();

    /**
     * Get if the player should be sent to the
     * servers in lobby servers or auth servers
     *
     * @return if the servers are enabled
     */
    public abstract boolean sendToServers();

    /**
     * Get the proxy ID
     *
     * @return the proxy server ID
     */
    public abstract UUID getProxyID();

    /**
     * Get all the lobby servers
     *
     * @return all the available lobby servers
     */
    public abstract Iterator<Object> lobbyServers();

    /**
     * Get all the auth servers
     *
     * @return all the available auth servers
     */
    public abstract Iterator<Object> authServer();
}
