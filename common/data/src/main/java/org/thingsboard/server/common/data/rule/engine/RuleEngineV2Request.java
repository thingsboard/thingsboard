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
package org.thingsboard.server.common.data.rule.engine;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

@Data
public class RuleEngineV2Request {

    @Schema(description = "Originator of the forwarded TbMsg. When omitted, the calling User's id is used.")
    private EntityId originator;

    @Schema(description = "Optional rule engine queue name. Overrides the queue selected by device/asset profile when present.")
    private String queueName;

    @Schema(description = "Timeout to process the request, in milliseconds. When omitted or <= 0, the platform default is used.",
            example = "10000")
    private int timeout;

    @Schema(description = "Message payload forwarded to the rule engine as TbMsg.data.", requiredMode = Schema.RequiredMode.REQUIRED)
    private JsonNode payload;

    @Schema(description = "Optional list of entities for which to compute the ACL snapshot. " +
            "Size is bounded by the rule-engine.acl.max-entities configuration.")
    private List<EntityId> aclEntities;
}
