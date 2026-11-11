// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class Agent extends BaseDataWithAdditionalInfo<AgentId> implements GroupEntity<AgentId>, HasCustomerId, HasOwnerId, HasVersion {

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT;
    }


    private TenantId tenantId;
    private CustomerId customerId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "description")
    private String description;
    @NoXss
    @Length(fieldName = "routingKey")
    private String routingKey;
    @NoXss
    @Length(fieldName = "secret")
    private String secret;
    private AgentProfileId agentProfileId;
    @Getter
    private Long version;

    public Agent() {
        super();
    }

    public Agent(AgentId id) {
        super(id);
    }

    public Agent(Agent agent) {
        super(agent);
        this.tenantId = agent.getTenantId();
        this.customerId = agent.getCustomerId();
        this.name = agent.getName();
        this.description = agent.getDescription();
        this.routingKey = agent.getRoutingKey();
        this.secret = agent.getSecret();
        this.agentProfileId = agent.getAgentProfileId();
        this.version = agent.getVersion();
    }

    @Schema(description = "JSON object with the Agent Id. " +
            "Specify this field to update the Agent. " +
            "Referencing non-existing Agent Id will cause error. " +
            "Omit this field to create new Agent." )
    @Override
    public AgentId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the agent creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id. Use 'assignAgentToTenant' to change the Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return this.tenantId;
    }

    @Schema(description = "JSON object with Customer Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public CustomerId getCustomerId() {
        return this.customerId;
    }

    @Schema(description = "JSON object with Customer or Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public EntityId getOwnerId() {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @Override
    public void setOwnerId(EntityId entityId) {
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) {
            this.customerId = new CustomerId(entityId.getId());
        } else {
            this.customerId = new CustomerId(CustomerId.NULL_UUID);
        }
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Unique Agent Name in scope of Tenant", example = "Silo_A_Agent")
    @Override
    public String getName() {
        return this.name;
    }

    @Schema(description = "Agent description")
    public String getDescription() {
        return this.description;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Agent routing key used for authentication", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    public String getRoutingKey() {
        return this.routingKey;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Agent secret used for authentication")
    public String getSecret() {
        return this.secret;
    }

    @Schema(description = "JSON object with Agent Profile Id. Nullable.")
    public AgentProfileId getAgentProfileId() {
        return this.agentProfileId;
    }
}
