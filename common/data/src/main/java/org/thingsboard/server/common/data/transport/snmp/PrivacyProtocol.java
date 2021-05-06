/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
