package com.gexterio.webchat.server;

import com.gexterio.webchat.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import static com.gexterio.webchat.Command.*;

public class ClientHandler {
    private Socket socket;
    private ChatServer server;
    private DataInputStream in;
    private DataOutputStream out;
    private String nick;
    private AuthService authService;


    public ClientHandler(Socket socket, ChatServer server, AuthService authService) {
        try {
            this.socket = socket;
            this.server = server;
            this.authService = authService;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    authenticate();
                    readMessages();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void authenticate() {
        while (true) {
            try {
                final String message = in.readUTF();
                if (Command.isCommand(message)) {
                    Command command = Command.getCommand(message);
                    if (command == AUTH) {
                        final String[] params = command.parse(message);
                        String login = params[0];
                        String password = params[1];
                        final String nick = authService.
                                getNickByLoginAndPassword(login, password);
                        if (nick != null) {
                            if (server.isNickBusy(nick)) {
                                sendMessage(ERROR, "Пользователь уже авторизован");
                                continue;
                            }
                            sendMessage(AUTHOK, nick);
                            this.nick = nick;
                            server.broadcast(MESSAGE, "Пользователь " + nick + " зашел в чат");
                            server.subscribe(this);
                            break;
                        } else {
                            sendMessage(ERROR, "Неверные логин и пароль");
                        }
                    }
                }
            }  catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void readMessages() {
        while (true) {
            try {
                final String message = in.readUTF();
                final Command command = getCommand(message);
                if (command == Command.END) {
                    break;
                }
                if(command == PRIVATE_MESSAGE) {
                   final String[] params = command.parse(message);
                    String msg = params[1];
                    String nick = params[0];
                    server.sendPrivateMessage(this, nick, msg);
                   continue;
                }
                server.broadcast(MESSAGE,nick + ": " + command.parse(message)[0]);
            } catch (IOException e) {
                e.printStackTrace();
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
        sendMessage(Command.END);
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
            server.unsubscribe(this);
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

    public String getNick() {
        return nick;
    }
}
