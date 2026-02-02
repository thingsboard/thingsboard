/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
