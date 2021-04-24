package ml.karmaconfigs.locklogin.plugin.common.security.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public final class IpData {

    private final InetAddress address;

    private final static Map<byte[], Integer> clones = new HashMap<>();

    /**
     * Initialize the IP data
     *
     * @param ip the ip address
     */
    public IpData(final InetAddress ip) {
        address = ip;
    }

    /**
     * Add a clone to the clones amount
     */
    public final void addClone() {
        clones.put(address.getAddress(), getClonesAmount() + 1);
    }

    /**
     * Remove a clone from the clones amount
     */
    public final void delClone() {
        clones.put(address.getAddress(), getClonesAmount() - 1);
    }

    /**
     * Get the clones amount of the specified ip
     *
     * @return the ip clones amount
     */
    public final int getClonesAmount() {
        return clones.getOrDefault(address.getAddress(), 0);
    }
}
