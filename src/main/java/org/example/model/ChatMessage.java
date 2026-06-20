package org.example.model;

public class ChatMessage {

    private int id;
    private String username;
    private String text;

    public ChatMessage(int id, String username, String text) {
        this.id = id;
        this.username = username;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getText() {
        return text;
    }
}
