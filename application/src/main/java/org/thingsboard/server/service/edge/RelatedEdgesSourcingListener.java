// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.dao.edge.RelatedEdgesService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RelatedEdgesSourcingListener {

    private final RelatedEdgesService relatedEdgesService;

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        log.debug("RelatedEdgesSourcingListener initiated");
        executorService = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("related-edges-listener"));
    }

    @PreDestroy
    public void destroy() {
        log.debug("RelatedEdgesSourcingListener destroy");
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionEntityEvent<?> event) {
        executorService.submit(() -> {
            log.trace("[{}] ActionEntityEvent called: {}", event.getTenantId(), event);
            try {
                switch (event.getActionType()) {
                    case ASSIGNED_TO_EDGE, UNASSIGNED_FROM_EDGE -> relatedEdgesService.publishRelatedEdgeIdsEvictEvent(event.getTenantId(), event.getEntityId());
                }
            } catch (Exception e) {
                log.error("[{}] failed to process ActionEntityEvent: {}", event.getTenantId(), event, e);
            }
        });
    }

    @TransactionalEventListener(
            fallbackExecution = true,
            condition = "#event.entityId.getEntityType() != T(org.thingsboard.server.common.data.EntityType).AI_MODEL"
    )
    public void handleEvent(DeleteEntityEvent<?> event) {
        executorService.submit(() -> {
            log.trace("[{}] DeleteEntityEvent called: {}", event.getTenantId(), event);
            try {
                relatedEdgesService.publishRelatedEdgeIdsEvictEvent(event.getTenantId(), event.getEntityId());
            } catch (Exception e) {
                log.error("[{}] failed to process DeleteEntityEvent: {}", event.getTenantId(), event, e);
            }
        });
    }

}
