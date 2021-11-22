package com.alexProject;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TCPConnection {
    private final Socket socket;
    private final Thread receiverThread;
    private final Thread senderThread;
    private final BufferedWriter writer;
    private final BufferedReader reader;
    private int counter = 0;
    private static final Logger logToConsole = LogManager.getLogger("ToConsole");
    private static final Logger logToFile = LogManager.getLogger("ToLogFile");
    private static final Logger log = LogManager.getLogger("ToFileLogsForDebug");

    public TCPConnection(Socket socket) throws IOException {
        this.socket = socket;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        receiverThread = new Thread(this::clientReceiver);
        receiverThread.start();

        senderThread = new Thread(this::clientSender);
        senderThread.start();
    }

    private void clientSender() {
        while (!socket.isClosed()) {
            String messageToClientEver10sec = System.lineSeparator() + "Counter " + counter++ + " Time: " + currentDatetime();
            sendString(messageToClientEver10sec);

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("Exeption" + e);
            }
        }
    }

    private void clientReceiver() {
        try {
            onClientConnection();
            while (!receiverThread.isInterrupted()) {
                receiveString(reader.readLine());
            }
        } catch (IOException | NullPointerException | URISyntaxException e) {
            Thread.currentThread().interrupt();
            disconnect();
            log.debug("Exeption" + e);
        }
    }

    private String currentDatetime() {
        SimpleDateFormat sdt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return sdt.format(new Date());
    }

    private void sendString(String str) {
        try {
            writer.write(str + System.lineSeparator());
            writer.flush();

        } catch (IOException e) {
            log.error("Error", e);
        }
    }

    private void onClientConnection() {
        sendString("Client connected " + this);
        logToConsole.info("Client connected: " + this);
    }

    private Map<String, String> commandsInMap() throws IOException, URISyntaxException {
        URL url = URLClassLoader.getSystemResource("Commands");
        Path path = Paths.get(url.toURI());
        Map<String, String> mapComnds = new HashMap<>();
        Stream<String> lines = Files.lines(path);
        lines.filter(line -> line.contains(":")).forEach(
                line -> mapComnds.put(line.split(":")[1], line.split(":")[0]));
        return mapComnds;
    }

    private void receiveString(String clientRequest) throws IOException, URISyntaxException {
        if (commandsInMap().containsKey(clientRequest)) {
            for (Map.Entry<String, String> entry : commandsInMap().entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if (key.equals(clientRequest)) {
                    sendString(val);
                    logToFile.info("Client:{} Message:{}  {}", this, clientRequest, val);
                }
            }
        } else if (clientRequest.matches("[0-9]+")) {
            int requestInt = Integer.parseInt(clientRequest);
            int responseInt = requestInt * 1000;
            String response = " Server answer:" + clientRequest + " -> " + responseInt;
            sendString(response);
            logToFile.info("Client:{} Message:{} {}", this, clientRequest, response);
        } else if (clientRequest.equalsIgnoreCase("exit")) {
            disconnect();
        } else if (clientRequest.equals("")) {
            return;
        } else {
            int k = 1;
            String toLowerCase = clientRequest.toLowerCase();
            char[] chars = toLowerCase.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (Character.isLetter(c)) {
                    k++;
                }
                if (k % 2 == 0) {
                    chars[i] = Character.toUpperCase(c);
                }
            }
            String responseFromArray = Stream.of(chars).map(arr -> new String(chars)).collect(Collectors.joining());
            String responseReplaced = responseFromArray.replaceAll(" ", "_");
            String response = " Server answer:" + " -> " + responseReplaced;
            sendString(response);
            logToFile.info("Client:{} Message:{} {}", this, clientRequest, response);
        }
        logToConsole.info("Client:{}  Message:{} ", this, clientRequest);
    }

    private void disconnect() {
        senderThread.interrupt();
        receiverThread.interrupt();
        try {
            logToConsole.info("Client:{}  - disconnected", this);
            socket.close();
        } catch (IOException e) {
            log.error("Error on disconnect ", e);
        }
    }

    @Override
    public String toString() {
        return "TCPConnection:" + socket.getInetAddress() + " port:" + socket.getPort();
    }
}