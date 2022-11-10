package eu.locklogin.api.common.utils.plugin;

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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * LockLogin registered servers data storager
 */
public final class ServerDataStorage {

    private static final Set<String> key_registered = new LinkedHashSet<>();
    private static final Set<String> proxy_registered = new LinkedHashSet<>();

    /**
     * Add a server name to the list of servers
     * which already have proxy key
     *
     * @param server the server name
     */
    public static void setKeyRegistered(final String server) {
        key_registered.add(server);
    }

    /**
     * Add a server name to the list of servers
     * which already have this proxy id
     *
     * @param server the server name
     */
    public static void setProxyRegistered(final String server) {
        proxy_registered.add(server);
    }

    /**
     * Remove a server name to the list of servers
     * which already have proxy key
     *
     * @param server the server name
     */
    public static void removeProxyRegistered(final String server) {
        proxy_registered.remove(server);
    }

    /**
     * Remove a server name to the list of servers
     * which already have this proxy id
     *
     * @param server the server name
     */
    public static void removeKeyRegistered(final String server) {
        key_registered.remove(server);
    }

    /**
     * Get if the server needs to retrieve a proxy key
     *
     * @param server the server name
     * @return if the server needs to know about the proxy key
     */
    public static boolean needsRegister(final String server) {
        return !key_registered.contains(server);
    }

    /**
     * Get if the server needs to know about this proxy
     * instance
     *
     * @param server the server name
     * @return if the server needs to know about this proxy
     */
    public static boolean needsProxyKnowledge(final String server) {
        return !proxy_registered.contains(server);
    }

    /**
     * Get if the server knows about the proxy
     * instance and is registered
     *
     * @param server the server
     * @return if the server has proxy knowledge
     */
    public static boolean hasProxyKnowledge(final String server) {
        return key_registered.contains(server) && proxy_registered.contains(server);
    }
}
