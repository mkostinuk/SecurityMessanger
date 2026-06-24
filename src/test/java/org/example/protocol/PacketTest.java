package org.example.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PacketTest {

    @Test
    void shouldKeepTypeAndDataAfterJsonRoundTrip() {
        Packet packet = new Packet(PacketType.CHAT_MSG)
                .put("username", "alice")
                .put("text", "hello");

        Packet parsed = Packet.fromJson(packet.toJson());

        assertEquals(PacketType.CHAT_MSG, parsed.getType());
        assertEquals("alice", parsed.get("username"));
        assertEquals("hello", parsed.get("text"));
    }

    @Test
    void shouldReturnNullForMissingKey() {
        Packet packet = new Packet(PacketType.LOGIN);

        assertNull(packet.get("missing"));
    }

    @Test
    void shouldParsePacketFromRawJson() {
        String json = "{\"type\":\"PRIVATE_MSG\",\"data\":{\"to\":\"bob\",\"payload\":\"xyz\"}}";

        Packet packet = Packet.fromJson(json);

        assertEquals(PacketType.PRIVATE_MSG, packet.getType());
        assertEquals("bob", packet.get("to"));
        assertEquals("xyz", packet.get("payload"));
    }
}
