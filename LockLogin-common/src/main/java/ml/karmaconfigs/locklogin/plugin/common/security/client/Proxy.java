package ml.karmaconfigs.locklogin.plugin.common.security.client;

import ml.karmaconfigs.api.common.Console;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Proxy {

    private final InetSocketAddress ip;
    private final static Set<String> proxies = new LinkedHashSet<>();

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
                InputStream input = Proxy.class.getResourceAsStream("/proxies.txt");
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
            } catch (Throwable ignored) {}
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
