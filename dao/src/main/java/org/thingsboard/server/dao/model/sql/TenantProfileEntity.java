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
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.TENANT_PROFILE_TABLE_NAME)
public final class TenantProfileEntity extends BaseSqlEntity<TenantProfile> {

    @Column(name = ModelConstants.TENANT_PROFILE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.TENANT_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.TENANT_PROFILE_IS_DEFAULT_PROPERTY)
    private boolean isDefault;

    @Column(name = ModelConstants.TENANT_PROFILE_ISOLATED_TB_RULE_ENGINE)
    private boolean isolatedTbRuleEngine;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.TENANT_PROFILE_PROFILE_DATA_PROPERTY)
    private JsonNode profileData;

    public TenantProfileEntity() {
        super();
    }

    public TenantProfileEntity(TenantProfile tenantProfile) {
        if (tenantProfile.getId() != null) {
            this.setUuid(tenantProfile.getId().getId());
        }
        this.setCreatedTime(tenantProfile.getCreatedTime());
        this.name = tenantProfile.getName();
        this.description = tenantProfile.getDescription();
        this.isDefault = tenantProfile.isDefault();
        this.isolatedTbRuleEngine = tenantProfile.isIsolatedTbRuleEngine();
        this.profileData = JacksonUtil.convertValue(tenantProfile.getProfileData(), ObjectNode.class);
    }

    @Override
    public TenantProfile toData() {
        TenantProfile tenantProfile = new TenantProfile(new TenantProfileId(this.getUuid()));
        tenantProfile.setCreatedTime(createdTime);
        tenantProfile.setName(name);
        tenantProfile.setDescription(description);
        tenantProfile.setDefault(isDefault);
        tenantProfile.setIsolatedTbRuleEngine(isolatedTbRuleEngine);
        tenantProfile.setProfileData(JacksonUtil.convertValue(profileData, TenantProfileData.class));
        return tenantProfile;
    }

}
