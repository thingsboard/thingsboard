/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.entityprofile.EntityProfile;
import org.thingsboard.server.common.data.id.EntityProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;

import static java.util.Optional.ofNullable;


@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.ENTITY_PROFILE_TABLE_FAMILY_NAME)
public class EntityProfileEntity extends BaseSqlEntity<EntityProfile> {
    @Column(name = ModelConstants.ENTITY_PROFILE_NAME_PROPERTY)
    private String name;
    @Column(name = ModelConstants.TENANT_ID_PROPERTY)
    private String tenantId;
    @Column(name = ModelConstants.ENTITY_TYPE_PROPERTY)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;
    @Type(type = "json")
    @Column(name = ModelConstants.ENTITY_PROFILE_PROFILE_PROPERTY)
    private JsonNode profile;
    @Type(type = "json")
    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public EntityProfileEntity(EntityProfile entityProfile) {
        ofNullable(entityProfile.getId()).map(UUIDBased::getId).ifPresent(this::setUuid);
        this.name = entityProfile.getName();
        ofNullable(entityProfile.getTenantId()).map(UUIDBased::getId).map(this::toString).ifPresent(this::setTenantId);
        this.entityType = entityProfile.getEntityType();
        this.profile = entityProfile.getProfile();
        this.additionalInfo = entityProfile.getAdditionalInfo();
    }

    @Override
    public EntityProfile toData() {
        return EntityProfile.builder()
                .id(new EntityProfileId(toUUID(id)))
                .name(name)
                .tenantId(tenantId == null ? null : new TenantId(toUUID(tenantId)))
                .entityType(entityType)
                .profile(profile)
                .additionalInfo(additionalInfo)
                .build();
    }
}
