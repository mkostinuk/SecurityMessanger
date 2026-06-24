package org.example.service;

import org.example.db.Database;
import org.example.model.Role;
import org.example.model.User;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    private final Database database;

    public AuthService(Database database) {
        this.database = database;
    }

    public User register(String username, String password, String displayName, String phone, String about) {
        if (database.findByUsername(username) != null) {
            return null;
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        int id = database.createUser(username, hash, Role.USER, displayName, phone, about);
        return new User(id, username, hash, Role.USER, false, displayName, phone, about);
    }

    public User login(String username, String password) {
        User user = database.findByUsername(username);
        if (user == null) {
            return null;
        }
        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            return null;
        }
        return user;
    }
}
