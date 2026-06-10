package org.example.protocol;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class Packet {

    private static final Gson GSON = new Gson();

    private PacketType type;
    private Map<String, String> data = new HashMap<>();

    public Packet() {
    }

    public Packet(PacketType type) {
        this.type = type;
    }

    public PacketType getType() {
        return type;
    }

    public Packet put(String key, String value) {
        data.put(key, value);
        return this;
    }

    public String get(String key) {
        return data.get(key);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static Packet fromJson(String json) {
        return GSON.fromJson(json, Packet.class);
    }
}
