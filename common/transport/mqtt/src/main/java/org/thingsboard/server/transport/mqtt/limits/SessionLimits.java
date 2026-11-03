// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.limits;

import lombok.Data;
import org.thingsboard.server.common.data.TransportPayloadType;

@Data
public class SessionLimits {

    private int maxPayloadSize;
    private int maxInflightMessages;
    private SessionRateLimits rateLimits;

    public record SessionRateLimits(String messages, String telemetryMessages, String telemetryDataPoints) {}
}
