// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
