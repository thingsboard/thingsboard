// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.transport.snmp;

import java.util.Arrays;
import java.util.Optional;

public enum AuthenticationProtocol {
    SHA_1("1.3.6.1.6.3.10.1.1.3"),
    SHA_224("1.3.6.1.6.3.10.1.1.4"),
    SHA_256("1.3.6.1.6.3.10.1.1.5"),
    SHA_384("1.3.6.1.6.3.10.1.1.6"),
    SHA_512("1.3.6.1.6.3.10.1.1.7"),
    MD5("1.3.6.1.6.3.10.1.1.2");

    // oids taken from org.snmp4j.security.SecurityProtocol implementations
    private final String oid;

    AuthenticationProtocol(String oid) {
        this.oid = oid;
    }

    public String getOid() {
        return oid;
    }

    public static Optional<AuthenticationProtocol> forName(String name) {
        return Arrays.stream(values())
                .filter(protocol -> protocol.name().equalsIgnoreCase(name))
                .findFirst();
    }
}
