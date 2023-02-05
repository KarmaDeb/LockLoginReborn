package eu.locklogin.api.common.utils.plugin;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.UUID;

public final class FloodGateUtil {

    private final UUID id;

    /**
     * Initialize the flood gate util
     *
     * @param clientID the client uuid
     */
    public FloodGateUtil(final UUID clientID) {
        id = clientID;
    }

    public static boolean hasFloodgate() {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Get if the player is a flood gate client
     * <p>
     * This method uses reflection.
     * In case there's no FloodGate plugin/server I want to make
     * sure to catch {@link ClassNotFoundException} error by using reflection
     *
     * @return if the player is a flood gate client
     */
    public boolean isBedrockClient() {
        FloodgateApi api = FloodgateApi.getInstance();
        return api.isFloodgatePlayer(id);
    }

    /**
     * Get if the java client of this player already
     * exists
     *
     * @return if the java player for this client
     * already exists
     */
    public boolean javaClientExists() {
        FloodgateApi api = FloodgateApi.getInstance();
        FloodgatePlayer player = api.getPlayer(id);
        return player.getLinkedPlayer() != null;
    }
}
