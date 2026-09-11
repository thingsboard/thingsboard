// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

@Data
@Schema
public class LifeCycleEventFilter implements EventFilter {

    @Schema(description = "String value representing the server name, identifier or ip address where the platform is running", example = "ip-172-31-24-152")
    protected String server;
    @Schema(description = "String value representing the lifecycle event type", example = "STARTED")
    protected String event;
    @Schema(description = "String value representing status of the lifecycle event", allowableValues = {"Success", "Failure"})
    protected String status;
    @Schema(description = "The case insensitive 'contains' filter based on error message", example = "not present in the DB")
    protected String errorStr;

    @Override
    public EventType getEventType() {
        return EventType.LC_EVENT;
    }

    @Override
    public boolean isNotEmpty() {
        return !StringUtils.isEmpty(server) || !StringUtils.isEmpty(event) || !StringUtils.isEmpty(status) || !StringUtils.isEmpty(errorStr);
    }
}
