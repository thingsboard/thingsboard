/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.entityconfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityConfig;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.entityconfig.EntityConfigDao;
import org.thingsboard.server.dao.model.sql.EntityConfigEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaEntityConfigDao extends JpaAbstractDao<EntityConfigEntity, EntityConfig> implements EntityConfigDao {

    @Autowired
    private EntityConfigRepository entityConfigRepository;

    @Override
    protected Class<EntityConfigEntity> getEntityClass() {
        return EntityConfigEntity.class;
    }

    @Override
    protected CrudRepository<EntityConfigEntity, UUID> getCrudRepository() {
        return entityConfigRepository;
    }

    @Override
    public EntityConfig findLatestEntityConfigByEntityId(TenantId tenantId, EntityId entityId) {
        Optional<EntityConfigEntity> entity =  entityConfigRepository.findFirstByTenantIdAndEntityIdOrderByCreatedTimeDesc(tenantId.getId(), entityId.getId());
        return DaoUtil.getData(entity);
    }

    @Override
    public PageData<EntityConfig> findEntityConfigsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        return DaoUtil.toPageData(entityConfigRepository.findEntityConfigs(tenantId.getId(), entityId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void removeByEntityId(TenantId tenantId, UUID id) {
        entityConfigRepository.deleteByTenantIdAndEntityId(tenantId.getId(), id);
    }
}
