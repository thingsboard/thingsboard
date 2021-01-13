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
package org.thingsboard.server.dao.entityconfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityConfig;
import org.thingsboard.server.common.data.id.EntityConfigId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import javax.transaction.Transactional;

@Service
@Slf4j
public class EntityConfigServiceImpl implements EntityConfigService {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private EntityConfigDao entityConfigDao;

    @Override
    public EntityConfig saveEntityConfigForEntity(TenantId tenantId, EntityId entityId, JsonNode configuration) {
        return saveEntityConfigForEntity(tenantId, entityId, configuration, null);
    }

    @Override
    @Transactional
    public void deleteEntityConfigsByEntityId(TenantId tenantId, EntityId entityId) {
        entityConfigDao.removeByEntityId(tenantId, entityId.getId());
    }

    @Override
    public EntityConfig saveEntityConfigForEntity(TenantId tenantId, EntityId entityId, JsonNode configuration, JsonNode additionalInfo) {
        if (configuration == null) {
            return null;
        }
        EntityConfig oldConfig = entityConfigDao.findLatestEntityConfigByEntityId(tenantId, entityId);
        if (oldConfig == null || !oldConfig.getConfiguration().equals(configuration)) {
            EntityConfig entityConfig = new EntityConfig();
            entityConfig.setConfiguration(configuration);
            long version = oldConfig == null ? 1L : oldConfig.getVersion() + 1;
            entityConfig.setVersion(version);
            entityConfig.setTenantId(tenantId);
            entityConfig.setEntityId(entityId);
            entityConfig.setCreatedTime(System.currentTimeMillis());
            entityConfig.setAdditionalInfo(additionalInfo);
            return entityConfigDao.save(tenantId, entityConfig);
        }
        return oldConfig;
    }

    @Override
    public EntityConfig getLatestEntityConfigByEntityId(TenantId tenantId, EntityId entityId) {
        return entityConfigDao.findLatestEntityConfigByEntityId(tenantId, entityId);
    }

    @Override
    public EntityConfig getEntityConfigById(TenantId tenantId, EntityConfigId entityConfigId) {
        return entityConfigDao.findById(tenantId, entityConfigId.getId());
    }

    @Override
    public EntityConfig restoreEntityConfig(TenantId tenantId, EntityId entityId, EntityConfigId entityConfigId, JsonNode additionalInfo) {
        EntityConfig entityConfig = getEntityConfigById(tenantId, entityConfigId);
        ObjectNode additionalInfoNode = additionalInfo != null ? (ObjectNode) additionalInfo : mapper.createObjectNode();
        additionalInfoNode.put("restoredFrom", entityConfig.getUuidId().toString());
        additionalInfoNode.put("restoredFromLabel", entityConfig.getVersion());
        return saveEntityConfigForEntity(tenantId, entityId, entityConfig.getConfiguration(), additionalInfo);
    }

    @Override
    public PageData<EntityConfig> getEntityConfigsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        return entityConfigDao.findEntityConfigsByEntityId(tenantId, entityId, pageLink);
    }

}
