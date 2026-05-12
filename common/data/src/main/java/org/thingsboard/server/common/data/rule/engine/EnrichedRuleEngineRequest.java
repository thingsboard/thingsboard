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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrichedRuleEngineRequest {

    @Schema(description = "Originator of the forwarded TbMsg. When omitted, the calling user's id is used.")
    private EntityId originator;

    @Schema(description = "Optional rule engine queue name. When present, overrides the queue selected by the " +
            "originator's profile.")
    private String queueName;

    @Schema(description = "Timeout to process the request, in milliseconds. When omitted, the platform default " +
            "(server.rest.rule_engine.response_timeout) is used.",
            example = "10000")
    private Integer timeout;

    @Schema(description = "Message payload forwarded to the rule engine as TbMsg.data. A null or missing payload " +
            "is treated as an empty JSON object '{}' so probe-only requests (callers who want only the ACL " +
            "snapshot) work without a body.")
    private JsonNode payload;

    @Schema(description = "Optional list of entities for which to compute the ACL snapshot. Each entry is the " +
            "platform's polymorphic EntityId form ({entityType, id}). Size is bounded by the configuration " +
            "property server.rest.rule_engine.acl.max_entities; exceeding the bound yields HTTP 400.")
    private List<EntityId> enrichEntities;

}
