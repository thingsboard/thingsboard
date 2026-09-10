// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

@Data
@Schema
public class StatisticsEventFilter implements EventFilter {

    @Schema(description = "String value representing the server name, identifier or ip address where the platform is running", example = "ip-172-31-24-152")
    protected String server;
    @Schema(description = "The minimum number of successfully processed messages", example = "25")
    protected Integer minMessagesProcessed;
    @Schema(description = "The maximum number of successfully processed messages", example = "250")
    protected Integer maxMessagesProcessed;
    @Schema(description = "The minimum number of errors occurred during messages processing", example = "30")
    protected Integer minErrorsOccurred;
    @Schema(description = "The maximum number of errors occurred during messages processing", example = "300")
    protected Integer maxErrorsOccurred;

    @Override
    public EventType getEventType() {
        return EventType.STATS;
    }

    @Override
    public boolean isNotEmpty() {
        return !StringUtils.isEmpty(server)
                || (minMessagesProcessed != null && minMessagesProcessed > 0) || (minErrorsOccurred != null && minErrorsOccurred > 0)
                || (maxMessagesProcessed != null && maxMessagesProcessed > 0) || (maxErrorsOccurred != null && maxErrorsOccurred > 0);
    }
}
