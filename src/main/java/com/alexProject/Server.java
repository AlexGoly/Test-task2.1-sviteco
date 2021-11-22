package com.alexProject;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {
    private static final Logger logToConsole = LogManager.getLogger("ToConsole");
    private static final Logger log = LogManager.getLogger("ToFileLogsForDebug");

    public static void main(String[] args) {
        new Server();
    }

    private Server() {
        logToConsole.info("Server start");
        try (ServerSocket serverSocket = new ServerSocket(8000)) {
            while (true) {
                try {
                    new TCPConnection(serverSocket.accept());

                } catch (IOException e) {
                    log.error("Error", e);
                }
            }
        } catch (IOException e) {
            log.error("Error", e);
            throw new RuntimeException(e);
        }
    }
}