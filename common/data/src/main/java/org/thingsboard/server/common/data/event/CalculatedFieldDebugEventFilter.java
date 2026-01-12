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
public class CalculatedFieldDebugEventFilter extends DebugEventFilter {

    @Schema(description = "String value representing the entity id in the event body", example = "57b6bafe-d600-423c-9267-fe31e5218986")
    protected String entityId;
    @Schema(description = "String value representing the entity type", allowableValues = "DEVICE")
    protected String entityType;
    @Schema(description = "String value representing the message id in the rule engine", example = "dcf44612-2ce4-4e5d-b462-ebb9c5628228")
    protected String msgId;
    @Schema(description = "String value representing the message type", example = "POST_TELEMETRY_REQUEST")
    protected String msgType;
    @Schema(description = "String value representing the arguments that were used in the calculation performed",
            example = "{\"x\":{\"ts\":1739432016629,\"value\":20},\"y\":{\"ts\":1739429717656,\"value\":12}}")
    protected String arguments;
    @Schema(description = "String value representing the result of a calculation",
            example = "{\"x + y\":32}")
    protected String result;

    @Override
    public EventType getEventType() {
        return EventType.DEBUG_CALCULATED_FIELD;
    }

    @Override
    public boolean isNotEmpty() {
        return super.isNotEmpty() || !StringUtils.isEmpty(entityId) || !StringUtils.isEmpty(entityType)
                || !StringUtils.isEmpty(msgId) || !StringUtils.isEmpty(msgType)
                || !StringUtils.isEmpty(arguments) || !StringUtils.isEmpty(result);
    }

}
