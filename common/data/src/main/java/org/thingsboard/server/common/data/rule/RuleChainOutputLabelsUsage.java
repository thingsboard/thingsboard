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
package org.thingsboard.server.common.data.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;

import java.util.Set;

@Schema
@Data
@Slf4j
public class RuleChainOutputLabelsUsage {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Rule Chain Id", accessMode = Schema.AccessMode.READ_ONLY)
    private RuleChainId ruleChainId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Rule Node Id", accessMode = Schema.AccessMode.READ_ONLY)
    private RuleNodeId ruleNodeId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Rule Chain Name", accessMode = Schema.AccessMode.READ_ONLY)
    private String ruleChainName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Rule Node Name", accessMode = Schema.AccessMode.READ_ONLY)
    private String ruleNodeName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Output labels", accessMode = Schema.AccessMode.READ_ONLY)
    private Set<String> labels;

}
