// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentProfileInfo extends AgentProfile {

    @Serial
    private static final long serialVersionUID = 7235198273645012983L;

    public AgentProfileInfo() {
        super();
    }

    public AgentProfileInfo(AgentProfile agentProfile) {
        super(agentProfile);
        // The Info endpoints are readable without an AGENT_PROFILE permission, so the auto-provision
        // credentials never travel on this projection - they are only served by getAgentProfileById.
        setProvisionKey(null);
        setProvisionSecret(null);
    }
}
