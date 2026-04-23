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
import lombok.Data;

import java.util.List;

/**
 * Body of a {@code POST /api/ruleEngine/v2/...} request.
 *
 * <p>{@link #payload} becomes the {@code TbMsg.data} forwarded to the Rule Engine.
 * {@link #enrichEntities} is an optional list (size bounded by configuration
 * {@code rule-engine.acl.max-entities}) of entities for which the platform computes
 * an ACL snapshot and writes it to the {@code tb_acl} metadata key.
 */
@Data
public class EnrichedRuleEngineRequest {

    private JsonNode payload;
    private List<EnrichEntityDescriptor> enrichEntities;

}
