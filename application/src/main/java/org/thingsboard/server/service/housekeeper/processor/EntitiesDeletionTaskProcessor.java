// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.housekeeper.EntitiesDeletionHousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntitiesDeletionTaskProcessor extends HousekeeperTaskProcessor<EntitiesDeletionHousekeeperTask> {

    private final EntityServiceRegistry entityServiceRegistry;

    @Override
    public void process(EntitiesDeletionHousekeeperTask task) throws Exception {
        EntityType entityType = task.getEntityType();
        TenantId tenantId = task.getTenantId();
        EntityDaoService entityService = entityServiceRegistry.getServiceByEntityType(entityType);

        for (UUID entityUuid : task.getEntities()) {
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityUuid);
            entityService.deleteEntity(tenantId, entityId, true);
        }
        log.debug("[{}] Deleted {} {}s", tenantId, task.getEntities().size(), entityType.getNormalName().toLowerCase());
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_ENTITIES;
    }

}
