package org.example.db;

import org.example.Config;
import org.example.model.ChatMessage;
import org.example.model.Role;
import org.example.model.User;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Database {

    public Database() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("no driver found", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                Config.get("db.url"),
                Config.get("db.user"),
                Config.get("db.password"));
    }

    public void initSchema() {
        String sql = readResource("schema.sql");
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("failed to create base schema", e);
        }
    }

    public User findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, role, is_banned FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            Role.valueOf(rs.getString("role")),
                            rs.getBoolean("is_banned"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("user search failed", e);
        }
    }

    public int createUser(String username, String passwordHash, Role role) {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException("error creating user", e);
        }
    }

    public int saveMessage(int senderId, String content) {
        String sql = "INSERT INTO messages (sender_id, content) VALUES (?, ?) RETURNING id";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setString(2, content);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException("error saving message", e);
        }
    }

    public void deleteMessage(int id) {
        String sql = "DELETE FROM messages WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("error deleting message", e);
        }
    }

    public List<ChatMessage> getRecentMessages(int limit) {
        String sql = "SELECT m.id, u.username, m.content FROM messages m " +
                "JOIN users u ON m.sender_id = u.id " +
                "ORDER BY m.id DESC LIMIT ?";
        List<ChatMessage> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new ChatMessage(rs.getInt("id"), rs.getString("username"), rs.getString("content")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("error loading messages", e);
        }
        Collections.reverse(result);
        return result;
    }

    private String readResource(String name) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new RuntimeException("no resource found: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("failed to read resource: " + name, e);
        }
    }
}
