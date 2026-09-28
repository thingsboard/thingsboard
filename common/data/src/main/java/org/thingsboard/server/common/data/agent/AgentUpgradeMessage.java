// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@Schema
public class AgentUpgradeMessage implements Serializable {

    private static final long serialVersionUID = 7845290732194618341L;

    @Schema(description = "Mapping of agent image tag to the tag it can be upgraded to.")
    private Map<String, AgentUpgradeInfo> agentVersions;
}
