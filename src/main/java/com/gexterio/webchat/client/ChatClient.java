package com.gexterio.webchat.client;

import com.gexterio.webchat.Command;
import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


import static com.gexterio.webchat.Command.*;

public class ChatClient {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final ChatController controller;
    private boolean isAuth;

    public ChatClient(ChatController controller) {
        this.controller = controller;
    }


    public void openConnection() throws IOException {
        socket = new Socket("localhost", 8999);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                waitAuth();
                readMessages();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeConnection();
                System.exit(0);
            }
        }).start();
    }

    private void waitAuth() throws IOException {

        while (true) {
            runTimer(120);
            final String message = in.readUTF();
            Command command = Command.getCommand(message);
            String[] params = command.parse(message);
            if (command == AUTHOK) {
                isAuth = true;
                final String nick = params[0];
                controller.setAuth(true);
                controller.addMessage("Успешная авторизация под ником " + nick);
                break;
            }
            if (command == ERROR) {
                Platform.runLater(() -> controller.showError(params[0]));
                continue;
            }
        }
    }

    private void runTimer(int maxSec) {
        new Thread (() -> {
            int timer = maxSec;
            while (timer>0) {
                try {
                    Thread.sleep(1000);
                    timer--;
                    System.out.println(timer);
                    if (isAuth) {
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (!isAuth) {
               closeConnection();

            }
        }).start();
    }

    private void readMessages() throws IOException {
        while (true) {
            final String message = in.readUTF();
            final Command command = Command.getCommand(message);
            if (END == command) {
                controller.setAuth(false);
                break;
            }
            String[] params = command.parse(message);
            if (ERROR == command) {
                String messageError = params[0];
                Platform.runLater(() -> controller.showError(messageError));
                continue;
            }
            if (MESSAGE == command) {
                Platform.runLater(() -> controller.addMessage(params[0]));
            }
            if (CLIENTS == command) {
                Platform.runLater(() -> controller.updateClientList(params));
            }
        }
    }

    private void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(Command command, String... params) {
        sendMessage(command.collectMessage(params));
    }
}
