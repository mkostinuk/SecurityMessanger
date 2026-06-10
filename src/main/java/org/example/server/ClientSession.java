package org.example.server;

import org.example.model.Role;

public class ClientSession {

    private int userId;
    private String username;
    private Role role;
    private boolean authenticated;

    public void login(int userId, String username, Role role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.authenticated = true;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }
}
