// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema
@Data
public class RuleChainData {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of the Rule Chain objects.", accessMode = Schema.AccessMode.READ_ONLY)
    List<RuleChain> ruleChains;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of the Rule Chain metadata objects.", accessMode = Schema.AccessMode.READ_ONLY)
    List<RuleChainMetaData> metadata;
}
