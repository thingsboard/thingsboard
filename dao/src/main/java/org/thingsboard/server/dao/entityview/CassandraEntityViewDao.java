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
import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.EntitySubtypeEntity;
import org.thingsboard.server.dao.model.nosql.EntityViewEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.*;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.*;

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

    @Override
    public List<EntityView> findEntityViewByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find entity-views by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<EntityViewEntity> entityViewEntities =
                findPageWithTextSearch(ENTITY_VIEW_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(ENTITY_VIEW_TENANT_ID_PROPERTY, tenantId)), pageLink);

        log.trace("Found entity-views [{}] by tenantId [{}] and pageLink [{}]", entityViewEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(entityViewEntities);
    }

    @Override
    public Optional<EntityView> findEntityViewByTenantIdAndName(UUID tenantId, String entityViewName) {
        return Optional.ofNullable(DaoUtil.getData(
                findOneByStatement(select().from(ENTITY_VIEW_TENANT_AND_NAME_VIEW_NAME).where()
                        .and(eq(ENTITY_VIEW_TENANT_ID_PROPERTY, tenantId))
                        .and(eq(ENTITY_VIEW_NAME_PROPERTY, entityViewName))))
        );
    }

    @Override
    public List<EntityView> findEntityViewByTenantIdAndEntityId(UUID tenantId, UUID entityId, TextPageLink pageLink) {
        log.debug("Try to find entity-views by tenantId [{}], entityId[{}] and pageLink [{}]", tenantId, entityId, pageLink);
        List<EntityViewEntity> entityViewEntities = findPageWithTextSearch(DEVICE_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(DEVICE_CUSTOMER_ID_PROPERTY, entityId),
                        eq(DEVICE_TENANT_ID_PROPERTY, tenantId)),
                pageLink);

        log.trace("Found entity-views [{}] by tenantId [{}], entityId [{}] and pageLink [{}]", entityViewEntities, tenantId, entityId, pageLink);
        return DaoUtil.convertDataList(entityViewEntities);
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
