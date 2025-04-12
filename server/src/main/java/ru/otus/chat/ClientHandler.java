package ru.otus.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ClientHandler {
    private Socket socket;
    private Server server;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;
    private Set<Roles> role;
    private static int userCount = 0;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.role = new HashSet<Roles>();

        userCount++;
        username = "user_" + userCount;
        if (userCount == 1) {
            role.add(Roles.ADMIN);
            role.add(Roles.USER);
        } else {
            role.add(Roles.USER);
        }

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
                        String[] strMessage = message.split(" ", 2);
                        if (strMessage[0].equals("/kick")) {
                            if (role.contains(Roles.ADMIN)) {
                                ClientHandler client = server.userByName(strMessage[1]);
                                if (client != null) {
                                        client.disconnect();
                                } else {
                                    sendMsg("Такого пользователя нет");
                                }
                            } else {
                                sendMsg("Недостаточно прав на выполнение команды");
                            }
                        }

                    } else {
                        server.broadcastMessage(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
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
}
