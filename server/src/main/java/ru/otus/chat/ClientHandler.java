package ru.otus.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Socket socket;
    private Server server;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;
    private static int userCount = 0;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        userCount++;
        username = "user_" + userCount;

        new Thread(() -> {
            try {
                System.out.println("Клиент " + username + " подключился");

                while (true) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equals("/exit")) {
                            sendMsg("/exitok");
                            break;
                        }

                    }
                    parseAndSendMsg(username, message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMsg(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUsername() {
        return username;
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void parseAndSendMsg(String username, String message) {
        String[] messageStrings = message.split(" ", 3);
        ClientHandler client = null;
        if (messageStrings[0].equals("/w")) {
            if (messageStrings.length == 3) {
                if (messageStrings[1] != null) {
                    client = server.userByName(messageStrings[1].trim());
                    if (client != null) {
                        if (messageStrings[2] != null) {
                            server.sendToUser(client, messageStrings[2]);
                        } else {
                            sendMsg("Отсутствует сообщение в команде /w");
                        }
                    } else {
                        sendMsg("Адресат не найден");
                    }
                } else {
                    sendMsg("Отсутствует адресат в команде /w");
                }
            } else if (messageStrings.length == 2) {
                sendMsg("Отсутствует сообщение в команде /w");
            } else if (messageStrings.length == 1) {
                sendMsg("Отсутствуют адресат и сообщение в команде /w");
            }
        } else {
            server.broadcastMessage(username + ": " + message);
        }
    }
}
