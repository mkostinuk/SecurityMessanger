package org.example.model;

public class User {

    private int id;
    private String username;
    private String passwordHash;
    private Role role;
    private boolean banned;
    private String displayName;
    private String phone;
    private String about;

    public User(int id, String username, String passwordHash, Role role, boolean banned,
                String displayName, String phone, String about) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.banned = banned;
        this.displayName = displayName;
        this.phone = phone;
        this.about = about;
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

    public String getDisplayName() {
        return displayName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAbout() {
        return about;
    }
}
