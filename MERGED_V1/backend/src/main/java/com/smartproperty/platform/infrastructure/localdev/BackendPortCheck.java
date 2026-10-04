package com.smartproperty.platform.infrastructure.localdev;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

final class BackendPortCheck {
    void verify(LocalDevelopmentConfiguration configuration) {
        String address = configuration.value("SERVER_ADDRESS", "127.0.0.1");
        int port;
        try {
            port = Integer.parseInt(configuration.value("SERVER_PORT", "8080"));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid SERVER_PORT", exception);
        }
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(address, port));
        } catch (IOException exception) {
            throw new IllegalStateException("Backend port " + address + ":" + port
                    + " is already occupied. The existing process was not stopped.", exception);
        }
        DevConsole.ok("Backend API port " + address + ":" + port + " is available");
    }
}
