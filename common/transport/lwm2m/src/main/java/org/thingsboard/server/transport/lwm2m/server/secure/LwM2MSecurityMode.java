package org.thingsboard.server.transport.lwm2m.server.secure;

public enum LwM2MSecurityMode {

    PSK(0, "psk"),
    RPK(1, "rpk"),
    X509(2, "x509"),
    NO_SEC(3, "no_sec"),
    X509_EST(4, "x509_est"),
    REDIS(5, "redis"),
    DEFAULT_MODE(100, "default_mode");

    public int code;
    public String  subEndpoint;

    LwM2MSecurityMode(int code, String subEndpoint) {
        this.code = code;
        this.subEndpoint = subEndpoint;
    }

    public static LwM2MSecurityMode fromSecurityMode(long code) {
        return fromSecurityMode((int) code);
    }

    public static LwM2MSecurityMode fromSecurityMode(int code) {
        for (LwM2MSecurityMode sm : LwM2MSecurityMode.values()) {
            if (sm.code == code) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported security code : %d", code));
    }

}
