package eu.locklogin.api.common.security.client;

import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Credentials holder
 */
public final class CredentialsHolder {

    private final static Map<UUID, Map<String, String>> credentials = new ConcurrentHashMap<>();

    private final ModulePlayer player;

    /**
     * Initialize the credentials holder
     *
     * @param client the client that is adding credentials
     */
    public CredentialsHolder(final ModulePlayer client) {
        player = client;
    }

    /**
     * Set a client credential
     *
     * @param name the credential name
     * @param value the credential value
     */
    public void storeCredential(final String name, final String value) {
        Map<String, String> data = credentials.getOrDefault(player.getUUID(), new ConcurrentHashMap<>());
        data.put(name, value);
        credentials.put(player.getUUID(), data);
    }

    /**
     * Get a client credential
     *
     * @param name the client credential name
     * @return the client credential
     */
    public String getCredential(final String name) {
        Map<String, String> data = credentials.getOrDefault(player.getUUID(), new ConcurrentHashMap<>());
        String value = data.getOrDefault(name, null);
        data.remove(name);

        credentials.put(player.getUUID(), data);
        return value;
    }
}
