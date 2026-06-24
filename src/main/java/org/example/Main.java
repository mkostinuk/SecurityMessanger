package org.example;

import org.example.db.Database;
import org.example.model.Role;
import org.example.server.ChatServer;

public class Main {
    public static void main(String[] args) {
        Database database = new Database();
        database.initSchema();
        seedAccounts(database);

        int port = Config.getInt("server.port");
        ChatServer server = new ChatServer(port, database);
        server.start();

        System.out.println("Server working on port: " + port);
    }

    private static void seedAccounts(Database database) {
        database.seedUser("admin", "admin123", Role.ADMIN, "Site Admin", "+380501112233", "I keep the order here");
        database.seedUser("moderator", "mod123", Role.MODERATOR, "Mod Helper", "+380502223344", "I clean up the chat");
        database.seedUser("alice", "alice123", Role.USER, "Alice", "+380503334455", "Just chatting");
        database.seedUser("bob", "bob123", Role.USER, "Bob", "+380504445566", "Hello everyone");
    }
}
