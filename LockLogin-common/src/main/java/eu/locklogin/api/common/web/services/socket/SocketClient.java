package eu.locklogin.api.common.web.services.socket;

import io.socket.client.Socket;

import java.net.URI;

public interface SocketClient {

    /**
     * Get the statistic server
     *
     * @return the statistic server
     */
    URI server();

    /**
     * Get the socket client
     *
     * @return the socket client
     */
    Socket client();
}
