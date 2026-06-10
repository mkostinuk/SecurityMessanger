package org.example.model;

public class User {

    private int id;
    private String username;
    private String passwordHash;
    private Role role;
    private boolean banned;

    public User(int id, String username, String passwordHash, Role role, boolean banned) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.banned = banned;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isBanned() {
        return banned;
    }
}
