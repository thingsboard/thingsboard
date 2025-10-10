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
        switch (event.getActionType()) {
            case ASSIGNED_TO_EDGE, UNASSIGNED_FROM_EDGE -> {
                executorService.submit(() -> {
                    log.trace("[{}] ActionEntityEvent called: {}", event.getTenantId(), event);
                    try {
                        relatedEdgesService.publishRelatedEdgeIdsEvictEvent(event.getTenantId(), event.getEntityId());
                    } catch (Exception e) {
                        log.error("[{}] failed to process ActionEntityEvent: {}", event.getTenantId(), event, e);
                    }
                });
            }
        }
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
