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
package org.thingsboard.server.service.edge.rpc.session.manager;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.edge.rpc.processor.PostgresGeneralEdgeEventsDispatcher;
import org.thingsboard.server.service.edge.rpc.session.EdgeSessionsHolder;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@ConditionalOnExpression("'${queue.type:null}'!='kafka'")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PostgresBasedEdgeGrpcSessionManager extends AbstractEdgeGrpcSessionManager {

    private final EdgeSessionsHolder edgeSessions;

    // todo: verify handled properly
    private final Lock newEventsLock = new ReentrantLock();
    private final AtomicReference<ScheduledFuture<?>> edgeEventCheckFutureRef = new AtomicReference<>();
    private PostgresGeneralEdgeEventsDispatcher generalEdgeEventsDispatcher;
    private volatile boolean hasNewEvents;

    @Override
    public void addEventToHighPriorityQueue(EdgeEvent edgeEvent) {
        super.addEventToHighPriorityQueue(edgeEvent);
        markHasNewEvents();
    }

    @Override
    public void onEdgeConnect() {
        markHasNewEvents();
        scheduleEdgeEventsCheck();
    }

    @Override
    public void onEdgeEventUpdate() {
        markHasNewEvents();
    }

    @Override
    public void onEdgeDisconnect() {
        markHasNoEvents();
        cancelScheduleEdgeEventsCheck();
    }

    @Override
    public void onEdgeRemoval() {
        markHasNoEvents();
        cancelScheduleEdgeEventsCheck();
    }

    @Override
    public boolean destroy() {
        markHasNoEvents();
        cancelScheduleEdgeEventsCheck();
        return true;
    }

    private void scheduleEdgeEventsCheck() {
        cancelScheduleEdgeEventsCheck();
        EdgeId edgeId = getState().getEdgeId();
        TenantId tenantId = getState().getTenantId();

        if (!edgeSessions.hasByEdgeId(edgeId)) {
            log.debug("[{}] Session was removed and edge event check schedule must not be started [{}]",
                    tenantId, edgeId.getId());
            return;
        }
        ScheduledFuture<?> edgeEventCheckTask = ctx.getEdgeEventProcessingExecutorService().schedule(() -> {
            try {
                newEventsLock.lock();
                try {
                    if (!hasNewEvents) {
                        scheduleEdgeEventsCheck();
                        return;
                    }
                    log.trace("[{}][{}] set session new events flag to false", tenantId, edgeId.getId());
                    hasNewEvents = false;
                    session.processHighPriorityEvents();
                    processEdgeEventsWithCallback(tenantId, edgeId);
                } finally {
                    newEventsLock.unlock();
                }
            } catch (Exception e) {
                log.warn("[{}] Failed to process edge events for edge [{}]!", tenantId, edgeId, e);
            }
        }, ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval(), TimeUnit.MILLISECONDS);

        edgeEventCheckFutureRef.set(edgeEventCheckTask);
        log.trace("[{}] Check edge event scheduled for edge [{}]", tenantId, edgeId.getId());
    }

    private void processEdgeEventsWithCallback(TenantId tenantId, EdgeId edgeId) throws Exception {
        Futures.addCallback(processEdgeEvents(), new FutureCallback<>() {
            @Override
            public void onSuccess(Boolean newEventsAdded) {
                if (Boolean.TRUE.equals(newEventsAdded)) {
                    log.trace("[{}][{}] new events added. set session new events flag to true", tenantId, edgeId.getId());
                    hasNewEvents = true;
                }
                scheduleEdgeEventsCheck();
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to process edge events for edge [{}]!", tenantId, edgeId, t);
                scheduleEdgeEventsCheck();
            }
        }, ctx.getGrpcCallbackExecutorService());
    }

    public ListenableFuture<Boolean> processEdgeEvents() throws Exception {
        if (generalEdgeEventsDispatcher == null) {
            generalEdgeEventsDispatcher = new PostgresGeneralEdgeEventsDispatcher(session, ctx);
        }
        return generalEdgeEventsDispatcher.processNewEvents();
    }

    private void cancelScheduleEdgeEventsCheck() {
        EdgeId edgeId = getState().getEdgeId();
        log.trace("[{}] cancelling edge event check for edge", edgeId);

        ScheduledFuture<?> sf = edgeEventCheckFutureRef.getAndSet(null);
        if (sf != null && !sf.isCancelled() && !sf.isDone()) {
            sf.cancel(true);
        }
    }

    private void markHasNoEvents() {
        markHasEvents(false);
    }

    private void markHasNewEvents() {
        markHasEvents(true);
    }

    private void markHasEvents(boolean newEventsPresent) {
        newEventsLock.lock();
        try {
            if (!hasNewEvents) {
                log.trace("[{}] set session new events flag to {} [{}]", getState().getTenantId(), newEventsPresent, getState().getEdgeId());
                hasNewEvents = newEventsPresent;
            }
        } finally {
            newEventsLock.unlock();
        }
    }

}
