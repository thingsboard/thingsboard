// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.edge.EdgeProfileData;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.EDGE_PROFILE_TABLE_NAME)
public class EdgeProfileEntity extends BaseVersionedEntity<EdgeProfile> {

    @Column(name = ModelConstants.EDGE_PROFILE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.EDGE_PROFILE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.EDGE_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.EDGE_PROFILE_IS_DEFAULT_PROPERTY)
    private boolean isDefault;

    @Column(name = ModelConstants.EDGE_PROFILE_ROOT_RULE_CHAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID rootRuleChainId;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.EDGE_PROFILE_PROFILE_DATA_PROPERTY, columnDefinition = "jsonb")
    private JsonNode profileData;

    public EdgeProfileEntity() {
        super();
    }

    public EdgeProfileEntity(EdgeProfile profile) {
        super(profile);
        if (profile.getTenantId() != null) {
            this.tenantId = profile.getTenantId().getId();
        }
        this.name = profile.getName();
        this.description = profile.getDescription();
        this.isDefault = profile.isDefault();
        if (profile.getRootRuleChainId() != null) {
            this.rootRuleChainId = profile.getRootRuleChainId().getId();
        }
        this.profileData = JacksonUtil.convertValue(profile.getProfileData(), ObjectNode.class);
    }

    @Override
    public EdgeProfile toData() {
        EdgeProfile profile = new EdgeProfile(new EdgeProfileId(id));
        profile.setCreatedTime(createdTime);
        profile.setVersion(version);
        if (tenantId != null) {
            profile.setTenantId(TenantId.fromUUID(tenantId));
        }
        profile.setName(name);
        profile.setDescription(description);
        profile.setDefault(isDefault);
        if (rootRuleChainId != null) {
            profile.setRootRuleChainId(new RuleChainId(rootRuleChainId));
        }
        profile.setProfileData(profileData != null ? JacksonUtil.convertValue(profileData, EdgeProfileData.class) : new EdgeProfileData());
        return profile;
    }
}
