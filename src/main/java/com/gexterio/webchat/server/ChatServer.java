package com.gexterio.webchat.server;

import com.gexterio.webchat.Command;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatServer {


    private final Map<String, ClientHandler> clients;

    public ChatServer() {
        this.clients = new HashMap<>();

    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(8999);
             AuthService authService = new InMemoryAuthService()) {
            while (true) {
                System.out.println("Ожидание подключения...");
                final Socket socket = serverSocket.accept();
                new ClientHandler(socket, this, authService);
                System.out.println("Клиент подключился");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(Command.MESSAGE, message);
        }
    }

//

    public void subscribe(ClientHandler client) {
        clients.put(client.getNick(), client);
        broadcastClientsList();
    }

    private void broadcastClientsList() {
        final String nicks = clients.values().stream()
                .map(ClientHandler::getNick)
                .collect(Collectors.joining(" "));
        broadcast(Command.CLIENTS, nicks);
    }

    public void broadcast(Command command, String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(command, message);
        }
    }

    public boolean isNickBusy(String nick) {
        return clients.get(nick) != null;
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client.getNick());
        broadcastClientsList();
    }

    public void sendPrivateMessage(ClientHandler from, String nickTo, String msg) {
        ClientHandler clientTo = clients.get(nickTo);
        if (clientTo == null) {
            from.sendMessage(Command.ERROR, "Пользователь не авторизован");
            return;
        }
        clientTo.sendMessage(Command.MESSAGE, "От "+ from.getNick() + ": " + msg);
        from.sendMessage(Command.MESSAGE, "Участнику " + nickTo + ": " + msg);
    }
}
