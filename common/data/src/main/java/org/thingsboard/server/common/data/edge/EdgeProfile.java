// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edge;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class EdgeProfile extends BaseData<EdgeProfileId> implements HasId<EdgeProfileId>, HasTenantId, HasVersion, HasName, TenantEntity {

    @Serial
    private static final long serialVersionUID = -127725937166894514L;

    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "description", max = 4096)
    private String description;
    private boolean isDefault;
    private RuleChainId rootRuleChainId;
    private EdgeProfileData profileData;
    @Getter
    private Long version;

    public EdgeProfile() {
        super();
    }

    public EdgeProfile(EdgeProfileId id) {
        super(id);
    }

    public EdgeProfile(EdgeProfile profile) {
        super(profile);
        this.tenantId = profile.getTenantId();
        this.name = profile.getName();
        this.description = profile.getDescription();
        this.isDefault = profile.isDefault();
        this.rootRuleChainId = profile.getRootRuleChainId();
        // deep copy: profiles are handed out from a shared cache, so a copied profile must never alias the cached lists
        this.profileData = profile.profileData != null ? new EdgeProfileData(profile.profileData) : null;
        this.version = profile.getVersion();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE_PROFILE;
    }

    @Schema(description = "JSON object with the Edge Profile Id.")
    @Override
    public EdgeProfileId getId() {
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

    @Schema(description = "Marks the default profile that is assigned to edges created without a profile.")
    public boolean isDefault() {
        return isDefault;
    }

    @Schema(description = "Root rule chain of every edge on this profile. Must be one of profileData.ruleChainIds, or null.")
    public RuleChainId getRootRuleChainId() {
        return rootRuleChainId;
    }

    @Schema(description = "Entity groups, rule chains, integrations and scheduler events assigned to every edge on this profile.")
    public EdgeProfileData getProfileData() {
        if (profileData == null) {
            profileData = new EdgeProfileData();
        }
        return profileData;
    }

}
