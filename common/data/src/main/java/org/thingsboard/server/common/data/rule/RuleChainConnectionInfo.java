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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.RuleChainId;

/**
 * Created by ashvayka on 21.03.18.
 */
@Schema
@Data
public class RuleChainConnectionInfo {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Index of rule node in the 'nodes' array of the RuleChainMetaData. Indicates the 'from' part of the connection.")
    private int fromIndex;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with the Rule Chain Id.")
    private RuleChainId targetRuleChainId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with the additional information about the connection.")
    private JsonNode additionalInfo;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type of the relation. Typically indicated the result of processing by the 'from' rule node. For example, 'Success' or 'Failure'")
    private String type;
}
