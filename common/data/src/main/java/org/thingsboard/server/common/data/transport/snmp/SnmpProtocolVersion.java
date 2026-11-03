// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.transport.snmp;

public enum SnmpProtocolVersion {
    V1(0),
    V2C(1),
    V3(3);

    private final int code;

    SnmpProtocolVersion(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
