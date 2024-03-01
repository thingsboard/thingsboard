/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanUpService {

    private final Optional<HousekeeperService> housekeeperService;
    private final RelationService relationService;

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEntityDeletionEvent(DeleteEntityEvent<?> event) {
        TenantId tenantId = event.getTenantId();
        EntityId entityId = event.getEntityId();
        log.trace("[{}] Handling entity deletion event: {}", tenantId, event);
        cleanUpRelatedData(tenantId, entityId);
        if (entityId.getEntityType() == EntityType.USER) {
            housekeeperService.ifPresent(housekeeperService -> {
                housekeeperService.submitTask(HousekeeperTask.unassignAlarms((User) event.getEntity()));
            });
        }
    }

    public void cleanUpRelatedData(TenantId tenantId, EntityId entityId) {
        // todo: skipped entities list
        relationService.deleteEntityRelations(tenantId, entityId);
        housekeeperService.ifPresent(housekeeperService -> {
            housekeeperService.submitTask(HousekeeperTask.deleteAttributes(tenantId, entityId));
            housekeeperService.submitTask(HousekeeperTask.deleteTelemetry(tenantId, entityId));
            housekeeperService.submitTask(HousekeeperTask.deleteEvents(tenantId, entityId));
            housekeeperService.submitTask(HousekeeperTask.deleteEntityAlarms(tenantId, entityId));
        });
    }

    public void removeTenantEntities(TenantId tenantId, EntityType... entityTypes) {
        UUID tasksKey = UUID.randomUUID(); // so that all tasks are pushed to single partition to be processed synchronously
        // todo: just use tenantId as key in the impl
        housekeeperService.ifPresent(housekeeperService -> {
            for (EntityType entityType : entityTypes) {
                housekeeperService.submitTask(tasksKey, HousekeeperTask.deleteEntities(tenantId, entityType));
            }
        });
    }

}
