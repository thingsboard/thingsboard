/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.stats.EntityStatistics;
import org.thingsboard.server.common.data.stats.EntityStatisticsValue;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.util.mapping.JsonBinaryType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.fromUuid;

@Data
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name = ModelConstants.ENTITY_STATISTICS_COLUMN_FAMILY_NAME)
@IdClass(EntityStatisticsEntity.CompositeKey.class)
public class EntityStatisticsEntity implements ToData<EntityStatistics> {

    @Id
    @Column(name = ModelConstants.ENTITY_ID_PROPERTY, nullable = false)
    private UUID entityId;

    @Id
    @Column(name = ModelConstants.ENTITY_TYPE_PROPERTY, nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    @Column(name = ModelConstants.TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.ENTITY_STATISTICS_LATEST_VALUE_PROPERTY)
    @Type(type = "jsonb")
    private JsonNode latestValue;

    @Column(name = ModelConstants.ENTITY_STATISTICS_TS_PROPERTY, nullable = false)
    private long ts;

    @Override
    public EntityStatistics toData() {
        EntityStatistics entityStatistics = new EntityStatistics();
        entityStatistics.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        entityStatistics.setTenantId(fromUuid(tenantId, TenantId::fromUUID));
        entityStatistics.setLatestValue(JacksonUtil.treeToValue(latestValue, EntityStatisticsValue.class));
        entityStatistics.setTs(ts);
        return entityStatistics;
    }

    @Data
    public static class CompositeKey implements Serializable {
        private UUID entityId;
        private EntityType entityType;
    }

}
