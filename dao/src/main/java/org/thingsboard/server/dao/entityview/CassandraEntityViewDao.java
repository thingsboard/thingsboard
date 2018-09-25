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
import com.google.common.util.concurrent.ListenableFuture;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_VIEW_BY_TENANT_AND_CUSTOMER_AND_SEARCH_TEXT;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_VIEW_BY_TENANT_AND_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_VIEW_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_VIEW_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_VIEW_TABLE_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_VIEW_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_PROPERTY;

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
    public List<EntityView> findEntityViewsByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find entity views by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<EntityViewEntity> entityViewEntities =
                findPageWithTextSearch(ENTITY_VIEW_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(TENANT_ID_PROPERTY, tenantId)), pageLink);
        log.trace("Found entity views [{}] by tenantId [{}] and pageLink [{}]",
                entityViewEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(entityViewEntities);
    }

    @Override
    public Optional<EntityView> findEntityViewByTenantIdAndName(UUID tenantId, String name) {
        Select.Where query = select().from(ENTITY_VIEW_BY_TENANT_AND_NAME).where();
        query.and(eq(ENTITY_VIEW_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(ENTITY_VIEW_NAME_PROPERTY, name));
        return Optional.ofNullable(DaoUtil.getData(findOneByStatement(query)));
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        log.debug("Try to find entity views by tenantId [{}], customerId[{}] and pageLink [{}]",
                tenantId, customerId, pageLink);
        List<EntityViewEntity> entityViewEntities = findPageWithTextSearch(
                ENTITY_VIEW_BY_TENANT_AND_CUSTOMER_AND_SEARCH_TEXT,
                Arrays.asList(eq(CUSTOMER_ID_PROPERTY, customerId), eq(TENANT_ID_PROPERTY, tenantId)),
                pageLink);
        log.trace("Found find entity views [{}] by tenantId [{}], customerId [{}] and pageLink [{}]",
                entityViewEntities, tenantId, customerId, pageLink);
        return DaoUtil.convertDataList(entityViewEntities);
    }

    @Override
    public ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(UUID tenantId, UUID entityId) {
        log.debug("Try to find entity views by tenantId [{}] and entityId [{}]", tenantId, entityId);
        Select.Where query = select().from(getColumnFamilyName()).where();
        query.and(eq(TENANT_ID_PROPERTY, tenantId));
        query.and(eq(ENTITY_ID_COLUMN, entityId));
        return findListByStatementAsync(query);
    }
}
