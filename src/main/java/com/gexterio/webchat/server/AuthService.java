package com.gexterio.webchat.server;

import java.io.Closeable;


public interface AuthService extends Closeable {
    String getNickByLoginAndPassword (String login, String password);
}
