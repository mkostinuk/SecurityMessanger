package org.example.protocol;

public enum PacketType {
    REGISTER,
    LOGIN,
    CHAT_MSG,
    PRIVATE_MSG,
    PUBLIC_KEY,
    ADMIN_ACTION,
    AUTH_SUCCESS,
    AUTH_FAIL,
    USER_JOINED,
    USER_LEFT,
    USER_LIST,
    ALL_USERS,
    HISTORY,
    MSG_DELETED,
    ERROR
}
