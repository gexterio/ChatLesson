package com.gexterio.webchat.server;

import com.gexterio.webchat.client.ChatController;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {

    private final List<ClientHandler> clients;

    public ChatServer() {
        this.clients = new ArrayList<>();

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
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void directMsg(String message) {
        String[] split = message.split("\\p{Blank}+");
        String nick = split[2];


        for(ClientHandler client : clients) {
            if (nick.equals(client.getNick())) {
                client.sendMessage(message);
            }
        }
    }

    public void subscribe(ClientHandler client) {
        clients.add(client);
    }

    public boolean isNickBusy(String nick) {
        for (ClientHandler client : clients) {
            if (nick.equals(client.getNick())) {
                return true;
            }
        }
        return false;
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
    }
}
