package eu.locklogin.api.account;

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
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockLogin client data
 */
@SuppressWarnings("unused")
public final class ClientData {

    private final static Map<UUID, Integer> id_amount = new ConcurrentHashMap<>();
    private final static Map<UUID, String[]> id_name = new ConcurrentHashMap<>();

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
    public void setVerified(final boolean status) {
        UUID id = UUID.randomUUID();
        if (ip != null) {
            id = UUID.nameUUIDFromBytes(ip.getAddress());
        }

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
    public int getVerified() {
        UUID id = UUID.randomUUID();
        if (ip != null) {
            id = UUID.nameUUIDFromBytes(ip.getAddress());
        }

        return id_amount.getOrDefault(id, 0);
    }

    /**
     * Get if the ip address is verified
     *
     * @return if the ip address is verified
     */
    public boolean notVerified() {
        UUID id = UUID.randomUUID();
        if (ip != null) {
            id = UUID.nameUUIDFromBytes(ip.getAddress());
        }

        return !verified.contains(id);
    }

    /**
     * Tries to assign the client
     *
     * @param maximum the maximum amount of clients
     * @param player the client
     * @return if the client could be assigned
     */
    public boolean canAssign(final int maximum, final ModulePlayer player) {
        UUID id = UUID.randomUUID();
        if (ip != null) {
            id = UUID.nameUUIDFromBytes(ip.getAddress());
        }

        String name = player.getName();
        String[] clients = id_name.getOrDefault(id, new String[0]);
        Set<String> clientSet = new HashSet<>(Arrays.asList(clients));
        int amount = id_amount.getOrDefault(id, 0);

        if (!clientSet.contains(name)) {
            if (amount < maximum) {
                clientSet.add(name);

                id_name.put(id, clientSet.toArray(new String[0]));
                id_amount.put(id, amount + 1);

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
     * @param player the client
     */
    public void removeClient(final ModulePlayer player) {
        UUID id = UUID.randomUUID();
        if (ip != null) {
            id = UUID.nameUUIDFromBytes(ip.getAddress());
        }

        String[] clients = id_name.getOrDefault(id, new String[0]);
        Set<String> clientSet = new HashSet<>(Arrays.asList(clients));
        int amount = id_amount.getOrDefault(id, 0);

        clientSet.remove(player.getName());

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
     * Generate a UUID from the ip
     *
     * @return the ip address UUID
     */
    public UUID getAddressID() {
        if (ip != null) {
            return UUID.nameUUIDFromBytes(ip.getAddress());
        }

        return UUID.randomUUID();
    }
}
