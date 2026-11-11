// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.AgentId;

import java.io.Serial;
import java.util.List;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentInfo extends Agent {

    @Serial
    private static final long serialVersionUID = -5509870435345957907L;

    @Schema(description = "Title of the Customer that owns the agent.", accessMode = Schema.AccessMode.READ_ONLY)
    private String customerTitle;
    @Schema(description = "Indicates special 'Public' Customer that is auto-generated to use the agents on public dashboards.", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean customerIsPublic;
    @Schema(description = "Name of the Agent Profile the agent belongs to.", accessMode = Schema.AccessMode.READ_ONLY)
    private String agentProfileName;
    @Schema(description = "Owner name — tenant title if the agent is owned by a tenant, or customer title otherwise.", accessMode = Schema.AccessMode.READ_ONLY)
    private String ownerName;
    @Schema(description = "Entity groups that contain this agent (excluding the implicit 'All' group).", accessMode = Schema.AccessMode.READ_ONLY)
    private List<EntityInfo> groups;
    @Schema(description = "Whether the agent currently has an active connection (derived from server-scope 'active' attribute).", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean active;
    @Schema(description = "Agent software version reported on the last connect (derived from server-scope 'agentVersion' attribute).", accessMode = Schema.AccessMode.READ_ONLY)
    private String agentVersion;
    @Schema(description = "Image reference this agent should be upgraded to, or null when no upgrade applies. Resolved from the published version graph.", accessMode = Schema.AccessMode.READ_ONLY)
    private String upgradeTargetImageRef;

    public AgentInfo() {
        super();
    }

    public AgentInfo(AgentId agentId) {
        super(agentId);
    }

    public AgentInfo(Agent agent, String customerTitle, boolean customerIsPublic, String agentProfileName) {
        super(agent);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
        this.agentProfileName = agentProfileName;
    }
}
