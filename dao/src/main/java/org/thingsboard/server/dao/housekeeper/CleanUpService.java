/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.housekeeper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.msg.housekeeper.HousekeeperClient;
import org.thingsboard.server.dao.eventsourcing.ActionCause;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanUpService {

    private final Optional<HousekeeperClient> housekeeperClient;
    private final RelationService relationService;

    private final Set<EntityType> skippedEntities = EnumSet.of(
            EntityType.ALARM, EntityType.QUEUE, EntityType.TB_RESOURCE, EntityType.OTA_PACKAGE,
            EntityType.NOTIFICATION_REQUEST, EntityType.NOTIFICATION_TEMPLATE,
            EntityType.NOTIFICATION_TARGET, EntityType.NOTIFICATION_RULE
    );

    @TransactionalEventListener(fallbackExecution = true) // after transaction commit
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handleEntityDeletionEvent(DeleteEntityEvent<?> event) {
        TenantId tenantId = event.getTenantId();
        EntityId entityId = event.getEntityId();
        EntityType entityType = entityId.getEntityType();
        try {
            log.trace("[{}][{}][{}] Handling entity deletion event", tenantId, entityType, entityId.getId());
            if (!skippedEntities.contains(entityType)) {
                cleanUpRelatedData(tenantId, entityId);
            }
            if (entityType == EntityType.USER && event.getCause() != ActionCause.TENANT_DELETION) {
                submitTask(HousekeeperTask.unassignAlarms((User) event.getEntity()));
            }
        } catch (Throwable e) {
            log.error("[{}][{}][{}] Failed to handle entity deletion event", tenantId, entityType, entityId.getId(), e);
        }
    }

    public void cleanUpRelatedData(TenantId tenantId, EntityId entityId) {
        log.debug("[{}][{}][{}] Cleaning up related data", tenantId, entityId.getEntityType(), entityId.getId());
        relationService.deleteEntityRelations(tenantId, entityId);
        submitTask(HousekeeperTask.deleteAttributes(tenantId, entityId));
        submitTask(HousekeeperTask.deleteTelemetry(tenantId, entityId));
        submitTask(HousekeeperTask.deleteEvents(tenantId, entityId));
        submitTask(HousekeeperTask.deleteAlarms(tenantId, entityId));
        submitTask(HousekeeperTask.deleteCalculatedFields(tenantId, entityId));
        if (Job.SUPPORTED_ENTITY_TYPES.contains(entityId.getEntityType())) {
            submitTask(HousekeeperTask.deleteJobs(tenantId, entityId));
        }
    }

    public void removeTenantEntities(TenantId tenantId, EntityType... entityTypes) {
        for (EntityType entityType : entityTypes) {
            submitTask(HousekeeperTask.deleteTenantEntities(tenantId, entityType));
        }
    }

    private void submitTask(HousekeeperTask task) {
        housekeeperClient.ifPresent(housekeeperClient -> {
            housekeeperClient.submitTask(task);
        });
    }

}
