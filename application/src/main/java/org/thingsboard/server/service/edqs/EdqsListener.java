// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edqs;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.RelationActionEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "queue.edqs.sync.enabled", havingValue = "true")
public class EdqsListener {

    private final EdqsService edqsService;

    @TransactionalEventListener(fallbackExecution = true)
    public void onUpdate(SaveEntityEvent<?> event) {
        if (event.getEntityId() == null || event.getEntity() == null) {
            return;
        }
        edqsService.onUpdate(event.getTenantId(), event.getEntityId(), event.getEntity());
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onDelete(DeleteEntityEvent<?> event) {
        if (event.getEntityId() == null) {
            return;
        }
        edqsService.onDelete(event.getTenantId(), event.getEntityId());
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(RelationActionEvent relationEvent) {
        if (relationEvent.getActionType() == ActionType.RELATION_ADD_OR_UPDATE) {
            edqsService.onUpdate(relationEvent.getTenantId(), ObjectType.RELATION, relationEvent.getRelation());
        } else if (relationEvent.getActionType() == ActionType.RELATION_DELETED) {
            edqsService.onDelete(relationEvent.getTenantId(), ObjectType.RELATION, relationEvent.getRelation());
        }
    }

}
