package com.alexProject;


import java.io.IOException;
import java.net.ServerSocket;

public class Server {
    public static void main(String[] args) {
        new Server();
    }

    private Server() {
        System.out.println("Serv start");
        try (ServerSocket serverSocket = new ServerSocket(8000)) {
            while (true) {
                try {
                    new TCPConection(serverSocket.accept());

                } catch (IOException e) {
                    System.out.println("TCP conection Exeption" + e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}