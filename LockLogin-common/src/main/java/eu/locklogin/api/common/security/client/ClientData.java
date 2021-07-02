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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockLogin client data
 */
public final class ClientData {

    private final static Map<UUID, Integer> id_amount = new ConcurrentHashMap<>();
    private final static Map<UUID, String[]> id_name = new ConcurrentHashMap<>();

    private final static Map<UUID, String> client_names = new ConcurrentHashMap<>();

    private final static Set<UUID> verified = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final InetAddress ip;

    /**
     * Initialize the verified client
     *
     * @param address the ip address of the verified client
     */
    public ClientData(final InetAddress address) {
        ip = address;
    }

    /**
     * Set the IP verification status
     *
     * @param status the ip verification status
     */
    public final void setVerified(final boolean status) {
        UUID id = getAddressID();

        if (status) {
            verified.add(id);
        } else {
            verified.remove(id);
            id_amount.remove(id);
            id_name.remove(id);
        }
    }

    /**
     * Get the amount of verified accounts
     * this IP has
     *
     * @return the amount of verified accounts the IP has
     */
    public final int getVerified() {
        return id_amount.getOrDefault(getAddressID(), 0);
    }

    /**
     * Get if the ip address is verified
     *
     * @return if the ip address is verified
     */
    public final boolean isVerified() {
        return verified.contains(getAddressID());
    }

    /**
     * Tries to assign the client
     *
     * @param maximum the maximum amount of clients
     * @param client the client name
     * @param clientID the client id
     * @return if the client could be assigned
     */
    public final boolean canAssign(final int maximum, final String client, final UUID clientID) {
        UUID id = getAddressID();

        String[] clients = id_name.getOrDefault(id, new String[0]);
        Set<String> clientSet = new HashSet<>(Arrays.asList(clients));
        int amount = id_amount.getOrDefault(id, 0);

        if (!clientSet.contains(client)) {
            if (amount < maximum) {
                clientSet.add(client);

                id_name.put(id, clientSet.toArray(new String[0]));
                id_amount.put(id, amount + 1);

                client_names.put(clientID, client);

                return true;
            }
        } else {
            return true;
        }

        return false;
    }

    /**
     * Remove a client from the verified clients
     *
     * @param client the client
     */
    public final void removeClient(final String client) {
        UUID id = getAddressID();

        String[] clients = id_name.getOrDefault(id, new String[0]);
        Set<String> clientSet = new HashSet<>(Arrays.asList(clients));
        int amount = id_amount.getOrDefault(id, 0);

        clientSet.remove(client);

        if (!clientSet.isEmpty()) {
            id_amount.put(id, amount - 1);
            id_name.put(id, clientSet.toArray(new String[0]));
        } else {
            //Un-verify ip if it gets empty
            verified.remove(id);
            id_amount.remove(id);
            id_name.remove(id);
        }
    }

    /**
     * Generate an UUID from the ip
     *
     * @return the ip address UUID
     */
    public final UUID getAddressID() {
        if (ip != null) {
            return UUID.nameUUIDFromBytes(ip.getAddress());
        }

        return UUID.randomUUID();
    }

    /**
     * Get the client name by its id
     *
     * @param id the client id
     * @return the client name
     */
    public static String getNameByID(final UUID id) {
        return client_names.getOrDefault(id, "");
    }
}
