// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.uplink;

public enum LwM2mTypeServer {
    BOOTSTRAP(0, "bootstrap"),
    CLIENT(1, "client");

    public int code;
    public String type;

    LwM2mTypeServer(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public static LwM2mTypeServer fromLwM2mTypeServer(String type) {
        for (LwM2mTypeServer sm : LwM2mTypeServer.values()) {
            if (sm.type.equals(type)) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported typeServer type : %d", type));
    }
}

