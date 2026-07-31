/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.attributes;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

/**
 * Propagates attribute updates to the edges related to the updated entity.
 * <p>
 * Without this service, attribute updates made on the cloud are propagated to the edge only if the tenant's
 * rule chains contain a 'push to edge' node that handles attribute messages. This service makes the synchronization
 * work out of the box for the entity types that require it (see AttributesEdgeSyncStrategy implementations).
 * <p>
 * General rules applied to all attribute updates before the entity type specific strategy is consulted:
 * <ul>
 *     <li>edges must be enabled;</li>
 *     <li>updates that originate from the edge are skipped, to avoid echoing the data back to the edge;</li>
 *     <li>updates that do not persist attributes are skipped.</li>
 * </ul>
 * The checks are lightweight and executed synchronously on the attributes save path. The actual synchronization
 * is executed on dedicated single-thread workers selected by the entity id - to avoid blocking the telemetry
 * callback threads and to preserve the per-entity order of the events. The sync task is submitted by the callback
 * of the attributes save itself, so no shared pool sits between the save and the worker and can reorder the events;
 * everything downstream of the worker preserves the order as well - the edge notification queue is partitioned
 * by the entity id and the edge events are delivered by seq id.
 * <p>
 * The order is therefore the order in which the writes completed, not the order in which they were issued.
 * For a caller that issues the updates sequentially these are the same, because the save is acknowledged only
 * after its future completes. Truly concurrent updates of the same attribute have no defined order on the cloud
 * either, so such conflicts are resolved on the edge by the update ts (see BaseTelemetryProcessor#filterAttributesByTs).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttributesEdgeSyncService {

    private final List<AttributesEdgeSyncStrategy> strategies;

    private final Map<EntityType, AttributesEdgeSyncStrategy> strategyByEntityType = new EnumMap<>(EntityType.class);
    private ExecutorService[] executors;

    @Value("${edges.enabled:false}")
    private boolean edgesEnabled;
    @Value("${edges.attributes_sync_pool_size:8}")
    private int poolSize;
    @Value("${edges.attributes_sync_max_queue_size:5000}")
    private int maxQueueSize;

    @PostConstruct
    public void init() {
        strategies.forEach(s -> strategyByEntityType.put(s.getEntityType(), s));
        if (edgesEnabled) {
            // single-thread workers selected by the entity id, so that the events of the same entity
            // are delivered to the edge in the order they were saved (e.g. save and delete of the same attribute)
            executors = new ExecutorService[poolSize];
            for (int i = 0; i < poolSize; i++) {
                executors[i] = ThingsBoardExecutors.newLimitedTasksExecutor(
                        1, maxQueueSize, "edge-attributes-sync-" + i, new ThreadPoolExecutor.AbortPolicy());
            }
        }
    }

    @PreDestroy
    public void destroy() {
        if (executors != null) {
            for (ExecutorService executor : executors) {
                executor.shutdownNow();
            }
        }
    }

    public boolean isEdgeSyncRequired(AttributesSaveRequest request) {
        if (!isEdgeSyncAllowed(request.getMsgSource()) || !request.getStrategy().saveAttributes()) {
            return false;
        }
        AttributesEdgeSyncStrategy syncStrategy = strategyByEntityType.get(request.getEntityId().getEntityType());
        return syncStrategy != null && syncStrategy.isEdgeSyncRequired(request);
    }

    public boolean isEdgeSyncRequired(AttributesDeleteRequest request) {
        if (!isEdgeSyncAllowed(request.getMsgSource())) {
            return false;
        }
        AttributesEdgeSyncStrategy syncStrategy = strategyByEntityType.get(request.getEntityId().getEntityType());
        return syncStrategy != null && syncStrategy.isEdgeSyncRequired(request);
    }

    /**
     * Syncs the update to the edges once the attributes are persisted.
     * Must be called only if {@link #isEdgeSyncRequired(AttributesSaveRequest)} returned true - the general rules
     * are not re-evaluated here.
     */
    public void onAttributesUpdate(AttributesSaveRequest request, ListenableFuture<?> saveFuture) {
        onAttributesEvent(request.getEntityId(), saveFuture,
                syncStrategy -> syncStrategy.onAttributesUpdate(request));
    }

    /**
     * Syncs the delete to the edges once the attributes are removed.
     * Must be called only if {@link #isEdgeSyncRequired(AttributesDeleteRequest)} returned true - the general rules
     * are not re-evaluated here.
     */
    public void onAttributesDelete(AttributesDeleteRequest request, ListenableFuture<?> deleteFuture) {
        onAttributesEvent(request.getEntityId(), deleteFuture,
                syncStrategy -> syncStrategy.onAttributesDelete(request));
    }

    private boolean isEdgeSyncAllowed(String msgSource) {
        return edgesEnabled && !DataConstants.EDGE_MSG_SOURCE.equals(msgSource);
    }

    private void onAttributesEvent(EntityId entityId, ListenableFuture<?> resultFuture, Consumer<AttributesEdgeSyncStrategy> action) {
        AttributesEdgeSyncStrategy syncStrategy = strategyByEntityType.get(entityId.getEntityType());
        if (syncStrategy == null || executors == null) {
            return;
        }
        Futures.addCallback(resultFuture, new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                try {
                    action.accept(syncStrategy);
                } catch (Exception e) {
                    log.error("[{}] Failed to sync attributes event to edge", entityId, e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                // attributes were not persisted - nothing to sync
            }
        }, executorByEntityId(entityId));
    }

    /**
     * The worker of the entity, wrapped to discard the event on overflow instead of letting the rejection surface
     * as an unhandled listener failure: the callback is dispatched when the write completes, long after it was
     * registered, so the overflow of the bounded queue can not be handled by the caller.
     */
    private Executor executorByEntityId(EntityId entityId) {
        ExecutorService executor = executors[Math.floorMod(entityId.hashCode(), executors.length)];
        return command -> {
            try {
                executor.execute(command);
            } catch (RejectedExecutionException e) {
                log.warn("[{}] Attributes edge sync queue is full, discarding the event", entityId);
            }
        };
    }

}
