package org.example.server;

import com.google.gson.Gson;
import org.example.db.Database;
import org.example.model.ChatMessage;
import org.example.model.User;
import org.example.protocol.Packet;
import org.example.protocol.PacketType;
import org.example.service.AuthService;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer extends WebSocketServer {

    private final Database database;
    private final AuthService auth;
    private final Gson gson = new Gson();
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
        if (session != null && session.isAuthenticated()) {
            broadcastUserList();
        }
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
                handleChat(conn, session, packet);
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

        if (isBlank(username) || isBlank(password)) {
            send(conn, new Packet(PacketType.AUTH_FAIL).put("message", "username and password are required"));
            return;
        }

        User user = auth.register(username, password);
        if (user == null) {
            send(conn, new Packet(PacketType.AUTH_FAIL).put("message", "username already taken"));
            return;
        }

        session.login(user.getId(), user.getUsername(), user.getRole());
        send(conn, new Packet(PacketType.AUTH_SUCCESS)
                .put("username", user.getUsername())
                .put("role", user.getRole().name()));
        sendHistory(conn);
        broadcastUserList();
        System.out.println("[Server] registered: " + username);
    }

    private void handleLogin(WebSocket conn, ClientSession session, Packet packet) {
        String username = packet.get("username");
        String password = packet.get("password");

        if (isBlank(username) || isBlank(password)) {
            send(conn, new Packet(PacketType.AUTH_FAIL).put("message", "username and password are required"));
            return;
        }

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
        sendHistory(conn);
        broadcastUserList();
        System.out.println("[Server] logged in: " + username);
    }

    private void handleChat(WebSocket conn, ClientSession session, Packet packet) {
        if (!session.isAuthenticated()) {
            return;
        }
        String text = packet.get("text");
        if (text == null || text.isBlank()) {
            return;
        }
        database.saveMessage(session.getUserId(), text);
        broadcast(new Packet(PacketType.CHAT_MSG)
                .put("username", session.getUsername())
                .put("text", text));
    }

    private void sendHistory(WebSocket conn) {
        List<ChatMessage> recent = database.getRecentMessages(50);
        send(conn, new Packet(PacketType.HISTORY).put("messages", gson.toJson(recent)));
    }

    private void broadcastUserList() {
        List<Map<String, String>> users = new ArrayList<>();
        for (ClientSession s : sessions.values()) {
            if (s.isAuthenticated()) {
                Map<String, String> u = new HashMap<>();
                u.put("username", s.getUsername());
                u.put("role", s.getRole().name());
                users.add(u);
            }
        }
        broadcast(new Packet(PacketType.USER_LIST).put("users", gson.toJson(users)));
    }

    private void broadcast(Packet packet) {
        for (Map.Entry<WebSocket, ClientSession> entry : sessions.entrySet()) {
            if (entry.getValue().isAuthenticated()) {
                send(entry.getKey(), packet);
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public void send(WebSocket conn, Packet packet) {
        if (conn.isOpen()) {
            conn.send(packet.toJson());
        }
    }
}
