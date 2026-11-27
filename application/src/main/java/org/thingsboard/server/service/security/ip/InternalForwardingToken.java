// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.ip;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Per-boot random token that authenticates the node's own loopback requests (the AI loopback
 * executor) as legitimate forwarders of a client address. Without it, forwarded headers on
 * loopback connections are ignored, so no other server-side process — such as an HTTP rule node
 * pointed at the node itself — can forge the resolved client address.
 */
public final class InternalForwardingToken {

    public static final String HEADER = "X-Internal-Forwarding-Token";

    private static final byte[] VALUE_BYTES = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
    private static final String VALUE = new String(VALUE_BYTES, StandardCharsets.UTF_8);

    private InternalForwardingToken() {
    }

    public static String value() {
        return VALUE;
    }

    public static boolean matches(String headerValue) {
        return headerValue != null && MessageDigest.isEqual(VALUE_BYTES, headerValue.getBytes(StandardCharsets.UTF_8));
    }

}
