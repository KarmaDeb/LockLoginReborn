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

import ml.karmaconfigs.api.common.Console;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * LockLogin proxy checker
 */
public final class Proxy {

    private final static Set<String> proxies = new LinkedHashSet<>();
    private final InetSocketAddress ip;

    /**
     * Initialize the proxy checker
     *
     * @param address the ip address
     */
    public Proxy(final InetSocketAddress address) {
        ip = address;
    }

    /**
     * Scan the internal file
     * to retrieve all the proxies
     */
    public static void scan() {
        if (proxies.isEmpty()) {
            try {
                InputStream input = Proxy.class.getResourceAsStream("/security/proxies.txt");
                if (input != null) {
                    InputStreamReader inReader = new InputStreamReader(input, StandardCharsets.UTF_8);
                    BufferedReader reader = new BufferedReader(inReader);

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.replaceAll("\\s", "").isEmpty()) {
                            proxies.add(line);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Check if the specified ip is a proxy
     *
     * @return if the address is a proxy
     */
    public final boolean isProxy() {
        if (ip != null) {
            String address = ip.getHostString();
            int port = ip.getPort();

            if (address.contains(":"))
                address = address.split(":")[0];

            for (String proxy : proxies) {
                if (proxy.contains(":")) {
                    String proxy_address = proxy.split(":")[0];
                    String proxy_port_string = proxy.replace(proxy_address + ":", "");

                    try {
                        int proxy_port = Integer.parseInt(proxy_port_string);

                        if (proxy_address.equals(address))
                            if (proxy_port == port)
                                return true;
                    } catch (Throwable ignored) {
                    }
                } else {
                    if (proxy.equals(address))
                        return true;
                }
            }

            return false;
        }

        Console.send("Couldn't process null ip proxy check at {0}", Instant.now().toString());
        return false;
    }
}
