// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.transport.snmp;

public enum SnmpMethod {
    GET(-96),
    SET(-93),
    TRAP(-89);

    // codes taken from org.snmp4j.PDU class
    private final int code;

    SnmpMethod(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
