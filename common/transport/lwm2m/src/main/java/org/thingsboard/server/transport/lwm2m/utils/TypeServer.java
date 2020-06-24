package org.thingsboard.server.transport.lwm2m.utils;

public enum TypeServer {
    BOOTSTRAP(0, "bootstrap"),
    SERVER(1, "server");

    public int code;
    public String type;

    TypeServer(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static TypeServer fromTypeServer(int code) {
        for (TypeServer sm : TypeServer.values()) {
            if (sm.code == code) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported type server : %d", code));
    }
}
