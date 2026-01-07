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
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema
public class RuleNodeDebugEventFilter extends DebugEventFilter {

    @Schema(description = "String value representing msg direction type (incoming to entity or outcoming from entity)", allowableValues = {"IN", "OUT"})
    protected String msgDirectionType;
    @Schema(description = "String value representing the entity id in the event body (originator of the message)", example = "de9d54a0-2b7a-11ec-a3cc-23386423d98f")
    protected String entityId;
    @Schema(description = "String value representing the entity type", allowableValues = "DEVICE")
    protected String entityType;
    @Schema(description = "String value representing the message id in the rule engine", example = "de9d54a0-2b7a-11ec-a3cc-23386423d98f")
    protected String msgId;
    @Schema(description = "String value representing the message type", example = "POST_TELEMETRY_REQUEST")
    protected String msgType;
    @Schema(description = "String value representing the type of message routing", example = "Success")
    protected String relationType;
    @Schema(description = "The case insensitive 'contains' filter based on data (key and value) for the message.", example = "humidity")
    protected String dataSearch;
    @Schema(description = "The case insensitive 'contains' filter based on metadata (key and value) for the message.", example = "deviceName")
    protected String metadataSearch;

    @Override
    public EventType getEventType() {
        return EventType.DEBUG_RULE_NODE;
    }

    @Override
    public boolean isNotEmpty() {
        return super.isNotEmpty() || !StringUtils.isEmpty(msgDirectionType) || !StringUtils.isEmpty(entityId)
                || !StringUtils.isEmpty(entityType) || !StringUtils.isEmpty(msgId) || !StringUtils.isEmpty(msgType) ||
                !StringUtils.isEmpty(relationType) || !StringUtils.isEmpty(dataSearch) || !StringUtils.isEmpty(metadataSearch);
    }
}
