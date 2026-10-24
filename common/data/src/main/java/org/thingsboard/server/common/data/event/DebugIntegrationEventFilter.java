// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.event;

import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

@Data
public class DebugIntegrationEventFilter extends DebugEventFilter {
    @Override
    public EventType getEventType() {
        return EventType.DEBUG_INTEGRATION;
    }

    private String type;
    private String message;
    private String statusIntegration;

    @Override
    public boolean isNotEmpty() {
        return super.isNotEmpty() || !StringUtils.isEmpty(type) || !StringUtils.isEmpty(message) || !StringUtils.isEmpty(statusIntegration);
    }
}
