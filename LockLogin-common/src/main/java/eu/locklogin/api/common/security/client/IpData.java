package eu.locklogin.api.common.security.client;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * LockLogin ip data
 */
public final class IpData {

    private final static Map<byte[], Integer> clones = new HashMap<>();
    private final InetAddress address;

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
