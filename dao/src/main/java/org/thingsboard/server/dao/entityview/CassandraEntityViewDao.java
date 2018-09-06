/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.dao.entityview;

import com.datastax.driver.core.Statement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.model.EntitySubtypeEntity;
import org.thingsboard.server.dao.model.nosql.EntityViewEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_VIEW_TABLE_FAMILY_NAME;

/**
 * Created by Victor Basanets on 9/06/2017.
 */
@Component
@Slf4j
@NoSqlDao
public class CassandraEntityViewDao extends CassandraAbstractSearchTextDao<EntityViewEntity, EntityView> implements EntityViewDao {

    @Override
    protected Class<EntityViewEntity> getColumnFamilyClass() {
        return EntityViewEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ENTITY_VIEW_TABLE_FAMILY_NAME;
    }

    @Override
    public EntityView save(EntityView domain) {
        EntityView savedEntityView = super.save(domain);
        EntitySubtype entitySubtype = new EntitySubtype(savedEntityView.getTenantId(), EntityType.ENTITY_VIEW,
                savedEntityView.getId().getEntityType().toString());
        EntitySubtypeEntity entitySubtypeEntity = new EntitySubtypeEntity(entitySubtype);
        Statement saveStatement = cluster.getMapper(EntitySubtypeEntity.class).saveQuery(entitySubtypeEntity);
        executeWrite(saveStatement);
        return savedEntityView;
    }

    /*Wasn't done!!!*/
    @Override
    public List<EntityView> findEntityViewByTenantId(UUID tenantId, TextPageLink pageLink) {

    }

    @Override
    public Optional<EntityView> findEntityViewByTenantIdAndName(UUID tenantId, String name) {
        return Optional.empty();
    }

    @Override
    public List<EntityView> findEntityViewByTenantIdAndEntityId(UUID tenantId, UUID entityId, TextPageLink pageLink) {
        return null;
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        return null;
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndCustomerIdAndEntityId(UUID tenantId, UUID customerId, UUID entityId, TextPageLink pageLink) {
        return null;
    }
}
