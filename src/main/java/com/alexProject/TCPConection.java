package com.alexProject;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class TCPConection {
    private final Socket socket;
    private Thread thread;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final BufferedWriter writerToLogFile;
    private int counter = 0;
    private final ArrayList<TCPConection> conections = new ArrayList<>();

    public TCPConection(Socket socket) throws IOException {
        this.socket = socket;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        writerToLogFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("C:/Alex project/test-task2.1-sviteco/src/main/resources/LogFile.txt", true)));

        thread = new Thread((new Runnable() {
            @Override
            public void run() {
                try {
                    newClientConection(TCPConection.this);
                    while (!thread.isInterrupted()) {
                        receiveString(reader.readLine());
                    }
                } catch (IOException e) {
                    System.out.println("TCP conection Exeption");
                    disconect();
                } finally {
                    clientDisconect(TCPConection.this);
                }
            }
        }));
        thread.start();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!thread.isInterrupted()) {
                    String messageToClientEver10sec = "\n\r" + "Counter " + counter++ + " Time: " + currentDatetime();
                    sendString(messageToClientEver10sec);
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        System.out.println("TCP conection Exeption");
                        disconect();
                    }
                }
            }
        });
        thread.start();
    }

    public String currentDatetime() {
        SimpleDateFormat sdt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return sdt.format(new Date());
    }

    public synchronized void sendString(String str) {
        try {
            writer.write(str + "\r\n");
            writer.flush();
        } catch (IOException e) {
            System.out.println("TCP conection Exeption");
            disconect();
        }
    }

    public void writeMessageToFileLog(String request, String response) {
        try {
            writerToLogFile.write("Client:" + socket.getInetAddress() + " port:" +
                    socket.getPort() + " message: " + request + response);
            writerToLogFile.newLine();
            writerToLogFile.flush();
        } catch (IOException e) {
            System.out.println("TCP conection Exeption");
            disconect();
        }
    }

    public synchronized void newClientConection(TCPConection tcpConection) {
        conections.add(tcpConection);
        sendToAllConnections("Client conected " + tcpConection);
        System.out.println("Client conected " + tcpConection);
    }

    public Map<String, String> commandsInMap() throws IOException {
        Path source = Paths.get("C:/Alex project/test-task2.1-sviteco/src/main/resources/Commands");
        Map<String, String> mapComnds = new HashMap<>();
        Stream<String> lines = Files.lines(source);
        lines.filter(line -> line.contains(":")).forEach(
                line -> mapComnds.put(line.split(":")[0], line.split(":")[1]));
        return mapComnds;
    }

    public synchronized void receiveString(String clientRequest) throws IOException {
        if (commandsInMap().containsValue(clientRequest)) {
            for (Map.Entry<String, String> entry : commandsInMap().entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if (val.equals(clientRequest)) {
                    sendString(key);
                    writeMessageToFileLog(clientRequest, " Server answer:" + key);
                }
            }
        } else if (clientRequest.matches("[0-9]+")) {
            int requestInt = Integer.parseInt(clientRequest);
            int responseInt = requestInt * 1000;
            String response = " Server answer:" + clientRequest + " -> " + responseInt;
            sendString(response);
            writeMessageToFileLog(clientRequest, response);
        } else if (clientRequest.equalsIgnoreCase("exit")) {
            clientDisconect(TCPConection.this);
            disconect();
        } else {
            String replaceStringlower = clientRequest.replaceAll(" +", "_").toLowerCase();
            String replaceStringUpper = clientRequest.replaceAll(" +", "_").toUpperCase();
            String response = " Server answer:" + replaceStringUpper + " -> " + replaceStringlower;
            sendString(response);
            writeMessageToFileLog(clientRequest, response);
        }
        System.out.println("Client:" + socket.getInetAddress() + " port:" + socket.getPort() + " message: " + clientRequest);
    }

    public synchronized void clientDisconect(TCPConection tcpConection) {
        conections.remove(tcpConection);
        System.out.println("Client " + tcpConection + " - disconected");
    }

    private void sendToAllConnections(String str) {
        for (TCPConection conection : conections) {
            conection.sendString(str);
        }
    }

    public synchronized void disconect() {
        thread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("TCP conection Exeption");
        }
    }

    @Override
    public String toString() {
        return "TCPConection: " + socket.getInetAddress() + " port:" + socket.getPort();
    }
}