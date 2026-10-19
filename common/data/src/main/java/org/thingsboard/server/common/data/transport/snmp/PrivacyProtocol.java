// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.transport.snmp;

import java.util.Arrays;
import java.util.Optional;

public enum PrivacyProtocol {
    DES("1.3.6.1.6.3.10.1.2.2"),
    AES_128("1.3.6.1.6.3.10.1.2.4"),
    AES_192("1.3.6.1.4.1.4976.2.2.1.1.1"),
    AES_256("1.3.6.1.4.1.4976.2.2.1.1.2");

    // oids taken from org.snmp4j.security.SecurityProtocol implementations
    private final String oid;

    PrivacyProtocol(String oid) {
        this.oid = oid;
    }

    public String getOid() {
        return oid;
    }

    public static Optional<PrivacyProtocol> forName(String name) {
        return Arrays.stream(values())
                .filter(protocol -> protocol.name().equalsIgnoreCase(name))
                .findFirst();
    }
}
