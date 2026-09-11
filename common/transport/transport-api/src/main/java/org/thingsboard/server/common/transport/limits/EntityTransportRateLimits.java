// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.limits;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EntityTransportRateLimits {

    private TransportRateLimit regularMsgRateLimit;
    private TransportRateLimit telemetryMsgRateLimit;
    private TransportRateLimit telemetryDataPointsRateLimit;

}
