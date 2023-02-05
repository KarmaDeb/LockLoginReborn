package eu.locklogin.api.common.web.alert.remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Remote proxy configuration
 */
public class RemoteProxy extends ProxyConfiguration {

    private final KarmaYamlManager manager;

    /**
     * Initialize the remote proxy
     *
     * @param simple_json the remote configuration
     */
    public RemoteProxy(final String simple_json) {
        Gson gson = new GsonBuilder().setLenient().create();
        Map<String, Object> map = gson.fromJson(simple_json, new TypeToken<Map<String, Object>>(){}.getType());

        KarmaYamlManager tmp = new KarmaYamlManager(map);
        KarmaYamlManager server = tmp.getSection("proxy");
        if (server.getKeySet().size() > 0) {
            tmp = server;
        }

        manager = tmp;
    }

    /**
     * Get if the proxy has multiple bungeecord
     * instances
     *
     * @return if the server has multiple bungeecord instances
     */
    @Override
    public boolean multiBungee() {
        return manager.getBoolean("Options.MultiBungee", CurrentPlatform.getRealProxyConfiguration().multiBungee());
    }

    /**
     * Get if the player should be sent to the
     * servers in lobby servers or auth servers
     *
     * @return if the servers are enabled
     */
    @Override
    public boolean sendToServers() {
        return manager.getBoolean("Options.SendToServers", CurrentPlatform.getRealProxyConfiguration().sendToServers());
    }

    /**
     * Get the proxy key used to register this
     * proxy instance
     *
     * @return the proxy key
     */
    @Override
    public String proxyKey() {
        return manager.getString("ProxyKey", CurrentPlatform.getRealProxyConfiguration().proxyKey());
    }

    /**
     * Get the proxy ID
     *
     * @return the proxy server ID
     */
    @Override
    public UUID getProxyID() {
        return UUID.fromString(manager.getString("ID", CurrentPlatform.getRealProxyConfiguration().getProxyID().toString()));
    }

    /**
     * Get all the lobby servers
     *
     * @param instance the server class instance
     * @return all the available lobby servers
     */
    @Override
    public <T> List<T> lobbyServers(final Class<T> instance) {
        return CurrentPlatform.getRealProxyConfiguration().lobbyServers(instance);
    }

    /**
     * Get all the auth servers
     *
     * @param instance the server class instance
     * @return all the available auth servers
     */
    @Override
    public <T> List<T> authServer(final Class<T> instance) {
        return CurrentPlatform.getRealProxyConfiguration().authServer(instance);
    }
}
