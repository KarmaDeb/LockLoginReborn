package eu.locklogin.api.common.utils.plugin;

import java.lang.reflect.Method;
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

    /**
     * Get if the player is a flood gate client
     *
     * This method uses reflection.
     * In case there's no FloodGate plugin/server I want to make
     * sure to catch {@link ClassNotFoundException} error by using reflection
     *
     * @return if the player is a flood gate client
     */
    public final boolean isFloodClient() {
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = api.getMethod("getInstance");

            Object instance = getInstance.invoke(api);
            Method getPlayer = instance.getClass().getMethod("getPlayer", UUID.class);

            Object player = getPlayer.invoke(instance, id);
            return player != null;
        } catch (Throwable ex) {
            return false;
        }
    }
}
