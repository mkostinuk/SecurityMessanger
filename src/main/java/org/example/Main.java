package org.example;

import org.example.db.Database;
import org.example.server.ChatServer;

public class Main {
    public static void main(String[] args) {
        Database database = new Database();
        database.initSchema();

        int port = Config.getInt("server.port");
        ChatServer server = new ChatServer(port, database);
        server.start();

        System.out.println("Server working on port: " + port);
    }
}
