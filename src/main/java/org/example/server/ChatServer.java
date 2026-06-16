package org.example.server;

import org.example.db.Database;
import org.example.model.User;
import org.example.protocol.Packet;
import org.example.protocol.PacketType;
import org.example.service.AuthService;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer extends WebSocketServer {

    private final Database database;
    private final AuthService auth;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private final Map<WebSocket, ClientSession> sessions = new ConcurrentHashMap<>();

    public ChatServer(int port, Database database) {
        super(new InetSocketAddress(port));
        this.database = database;
        this.auth = new AuthService(database);
    }

    @Override
    public void onStart() {
        System.out.println("[Server] WebSocket started");
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        sessions.put(conn, new ClientSession());
        System.out.println("[Server] new connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ClientSession session = sessions.remove(conn);
        String who = (session != null && session.isAuthenticated()) ? session.getUsername() : "unknown";
        System.out.println("[Server] connection closed (" + who + "), reason: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        threadPool.submit(() -> {
            try {
                Packet packet = Packet.fromJson(message);
                handlePacket(conn, packet);
            } catch (Exception e) {
                System.out.println("[Server] packet handling failed: " + e.getMessage());
                send(conn, new Packet(PacketType.ERROR).put("message", "invalid package"));
            }
        });
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("[Server] error: " + ex.getMessage());
    }

    private void handlePacket(WebSocket conn, Packet packet) {
        ClientSession session = sessions.get(conn);
        if (session == null) {
            return;
        }

        switch (packet.getType()) {
            case REGISTER:
                handleRegister(conn, session, packet);
                break;
            case LOGIN:
                handleLogin(conn, session, packet);
                break;
            case CHAT_MSG:
                break;
            case PRIVATE_MSG:
                break;
            case ADMIN_ACTION:
                break;
            default:
                send(conn, new Packet(PacketType.ERROR).put("message", "unknown package type"));
        }
    }

    private void handleRegister(WebSocket conn, ClientSession session, Packet packet) {
        String username = packet.get("username");
        String password = packet.get("password");

        User user = auth.register(username, password);
        if (user == null) {
            send(conn, new Packet(PacketType.AUTH_FAIL).put("message", "username already taken"));
            return;
        }

        session.login(user.getId(), user.getUsername(), user.getRole());
        send(conn, new Packet(PacketType.AUTH_SUCCESS)
                .put("username", user.getUsername())
                .put("role", user.getRole().name()));
        System.out.println("[Server] registered: " + username);
    }

    private void handleLogin(WebSocket conn, ClientSession session, Packet packet) {
        String username = packet.get("username");
        String password = packet.get("password");

        User user = auth.login(username, password);
        if (user == null) {
            send(conn, new Packet(PacketType.AUTH_FAIL).put("message", "incorrect login or password"));
            return;
        }
        if (user.isBanned()) {
            send(conn, new Packet(PacketType.AUTH_FAIL).put("message", "you are banned"));
            return;
        }

        session.login(user.getId(), user.getUsername(), user.getRole());
        send(conn, new Packet(PacketType.AUTH_SUCCESS)
                .put("username", user.getUsername())
                .put("role", user.getRole().name()));
        System.out.println("[Server] logged in: " + username);
    }

    public void send(WebSocket conn, Packet packet) {
        if (conn.isOpen()) {
            conn.send(packet.toJson());
        }
    }
}
