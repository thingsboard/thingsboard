// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class AgentProfile extends BaseData<AgentProfileId> implements HasId<AgentProfileId>, HasTenantId, HasVersion, HasName, TenantEntity {

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_PROFILE;
    }

    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "description")
    private String description;
    @NoXss
    @Length(fieldName = "provisionKey")
    private String provisionKey;
    @NoXss
    @Length(fieldName = "provisionSecret")
    private String provisionSecret;
    private AgentProvisionType provisionType;
    private boolean isDefault;
    @Getter
    private Long version;

    public AgentProfile() {
        super();
    }

    public AgentProfile(AgentProfileId id) {
        super(id);
    }

    public AgentProfile(AgentProfile profile) {
        super(profile);
        this.tenantId = profile.getTenantId();
        this.name = profile.getName();
        this.description = profile.getDescription();
        this.provisionKey = profile.getProvisionKey();
        this.provisionSecret = profile.getProvisionSecret();
        this.provisionType = profile.getProvisionType();
        this.isDefault = profile.isDefault();
        this.version = profile.getVersion();
    }

    @Schema(description = "JSON object with the Agent Profile Id.")
    @Override
    public AgentProfileId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the profile creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Unique profile name within tenant")
    @Override
    public String getName() {
        return name;
    }

    @Schema(description = "Profile description")
    public String getDescription() {
        return description;
    }

    @Schema(description = "Provision key for future auto-provisioning")
    public String getProvisionKey() {
        return provisionKey;
    }

    @Schema(description = "Provision secret for future auto-provisioning")
    public String getProvisionSecret() {
        return provisionSecret;
    }

    @Schema(description = "Provisioning strategy. DISABLED by default.")
    public AgentProvisionType getProvisionType() {
        return provisionType;
    }

    @Schema(description = "Used to mark the default profile that will be assigned to agents when no profile is specified.")
    public boolean isDefault() {
        return isDefault;
    }
}
