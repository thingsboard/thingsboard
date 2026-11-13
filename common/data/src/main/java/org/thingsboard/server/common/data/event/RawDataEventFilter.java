// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

@Data
@Schema
public class RawDataEventFilter implements EventFilter {

    @Schema(description = "String value representing the server name, identifier or ip address where the platform is running", example = "ip-172-31-24-152")
    protected String server;
    @Schema(description = "String value representing the uuid", example = "STARTED")
    protected String uuid;
    @Schema(description = "String value representing the message type")
    protected String messageType;
    @Schema(description = "String value representing the message")
    protected String message;

    @Override
    public EventType getEventType() {
        return EventType.RAW_DATA;
    }

    @Override
    public boolean isNotEmpty() {
        return !StringUtils.isEmpty(server) || !StringUtils.isEmpty(uuid) || !StringUtils.isEmpty(messageType) || !StringUtils.isEmpty(message);
    }
}
