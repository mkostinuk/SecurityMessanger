package org.example.server;

import com.google.gson.Gson;
import org.example.db.Database;
import org.example.model.ChatMessage;
import org.example.model.Role;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                handlePrivate(session, packet);
                break;
            case PUBLIC_KEY:
                handlePublicKey(session, packet);
                break;
            case ADMIN_ACTION:
                handleAdminAction(conn, session, packet);
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
        if (username.length() < 3 || username.length() > 20) {
            send(conn, new Packet(PacketType.AUTH_FAIL).put("message", "username must be 3-20 characters"));
            return;
        }
        if (password.length() < 6) {
            send(conn, new Packet(PacketType.AUTH_FAIL).put("message", "password must be at least 6 characters"));
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
        if (text == null || text.isBlank() || text.length() > 500) {
            return;
        }
        int id = database.saveMessage(session.getUserId(), text);
        broadcast(new Packet(PacketType.CHAT_MSG)
                .put("id", String.valueOf(id))
                .put("username", session.getUsername())
                .put("text", text));
    }

    private void handleAdminAction(WebSocket conn, ClientSession session, Packet packet) {
        if (!session.isAuthenticated()) {
            return;
        }
        String action = packet.get("action");

        if ("DELETE_MSG".equals(action)) {
            if (session.getRole() == Role.MODERATOR || session.getRole() == Role.ADMIN) {
                int id = Integer.parseInt(packet.get("messageId"));
                database.deleteMessage(id);
                broadcast(new Packet(PacketType.MSG_DELETED).put("id", String.valueOf(id)));
            }
        } else if ("BAN_USER".equals(action)) {
            if (session.getRole() == Role.ADMIN) {
                banUser(packet.get("target"));
            }
        } else if ("PROMOTE".equals(action)) {
            if (session.getRole() == Role.ADMIN) {
                promoteUser(packet.get("target"));
            }
        }
    }

    private void banUser(String target) {
        database.setBanned(target, true);
        for (Map.Entry<WebSocket, ClientSession> entry : sessions.entrySet()) {
            ClientSession s = entry.getValue();
            if (s.isAuthenticated() && s.getUsername().equals(target)) {
                send(entry.getKey(), new Packet(PacketType.ERROR).put("message", "you have been banned"));
                entry.getKey().close();
            }
        }
    }

    private void promoteUser(String target) {
        database.setRole(target, Role.MODERATOR);
        for (ClientSession s : sessions.values()) {
            if (s.isAuthenticated() && s.getUsername().equals(target)) {
                s.setRole(Role.MODERATOR);
            }
        }
        broadcastUserList();
    }

    private void handlePrivate(ClientSession session, Packet packet) {
        if (!session.isAuthenticated()) {
            return;
        }
        String to = packet.get("to");
        String payload = packet.get("payload");

        for (Map.Entry<WebSocket, ClientSession> entry : sessions.entrySet()) {
            ClientSession s = entry.getValue();
            if (s.isAuthenticated() && s.getUsername().equals(to)) {
                send(entry.getKey(), new Packet(PacketType.PRIVATE_MSG)
                        .put("from", session.getUsername())
                        .put("payload", payload));
            }
        }
    }

    private void handlePublicKey(ClientSession session, Packet packet) {
        if (!session.isAuthenticated()) {
            return;
        }
        session.setPublicKey(packet.get("key"));
        broadcastUserList();
    }

    private void sendHistory(WebSocket conn) {
        List<ChatMessage> recent = database.getRecentMessages(50);
        send(conn, new Packet(PacketType.HISTORY).put("messages", gson.toJson(recent)));
    }

    private void broadcastUserList() {
        List<Map<String, String>> users = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ClientSession s : sessions.values()) {
            if (s.isAuthenticated() && seen.add(s.getUsername())) {
                Map<String, String> u = new HashMap<>();
                u.put("username", s.getUsername());
                u.put("role", s.getRole().name());
                u.put("publicKey", s.getPublicKey() == null ? "" : s.getPublicKey());
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
