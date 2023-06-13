/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.eventsourcing.DeleteDaoEdgeEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteDaoEvent;
import org.thingsboard.server.dao.eventsourcing.EntityUpdateEvent;
import org.thingsboard.server.dao.eventsourcing.SaveDaoEvent;

import javax.annotation.PostConstruct;

/**
 * This event listener does not support async event processing because relay on ThreadLocal
 * Another possible approach is to implement a special annotation and a bunch of classes similar to TransactionalApplicationListener
 * This class is the simplest approach to maintain replica synchronization within the single class.
 * <p>
 * For async event publishers, you have to decide whether publish event on creating async task in the same thread where dao method called
 * @Autowired
 * EdgeEventSynchronizationManager edgeSynchronizationManager
 * ...
 *   //some async write action make future
 *   if (!edgeSynchronizationManager.isSync()) {
 *     future.addCallback(eventPublisher.publishEvent(...))
 *   }
 * */
@Component
@RequiredArgsConstructor
@Slf4j
public class EdgeEventSourcingListener {

    private final TbClusterService tbClusterService;
    private final EdgeSynchronizationManager edgeSynchronizationManager;

    @PostConstruct
    public void init() {
        log.info("EdgeEventSourcingListener initiated");
    }

    @EventListener
    public void failEvent(SaveDaoEvent.SaveDaoEventBuilder<?> event) {
        String message = "SaveDaoEvent.SaveDaoEventBuilder event is not allowed " + event;
        RuntimeException exception = new RuntimeException(message);
        log.error(message, exception);
        throw exception;
    }

    @EventListener
    public void failEvent(DeleteDaoEvent.DeleteDaoEventBuilder<?> event) {
        String message = "DeleteDaoEvent.DeleteDaoEventBuilder event is not allowed" + event;
        RuntimeException exception = new RuntimeException(message);
        log.error(message, exception);
        throw exception;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(SaveDaoEvent<?> event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        log.trace("SaveDaoEvent called: {}", event);
        tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, event.getEntityId(),
                JacksonUtil.toPrettyString(event.getEntity()), null, EdgeEventActionType.UPDATED);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteDaoEvent<?> event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        log.trace("DeleteDaoEvent called: {}", event);
        for (EdgeId edgeId : event.getRelatedEdgeIds()) {
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), edgeId, event.getEntityId(),
                    null, null, EdgeEventActionType.DELETED);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteDaoEdgeEvent<?> event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        log.trace("DeleteDaoEdgeEvent called: {}", event);
        tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, event.getEntityId(),
                null, null, EdgeEventActionType.DELETED);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(EntityUpdateEvent event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        log.trace("EntityRelationUpdateEvent called: {}", event);
        tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), event.getEdgeId(), event.getEntityId(),
                event.getBody(), null, event.getActionType());
    }
}
