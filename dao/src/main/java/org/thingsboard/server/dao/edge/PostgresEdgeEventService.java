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
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.dao.edge.stats.EdgeStatsCounterService;
import org.thingsboard.server.dao.edge.stats.EdgeStatsKey;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.type:null}'!='kafka'")
public class PostgresEdgeEventService extends BaseEdgeEventService {

    private final EdgeEventDao edgeEventDao;
    private final ApplicationEventPublisher eventPublisher;
    private final Optional<EdgeStatsCounterService> statsCounterService;

    private ExecutorService edgeEventExecutor;

    @PostConstruct
    public void initExecutor() {
        edgeEventExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edge-event-service"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (edgeEventExecutor != null) {
            edgeEventExecutor.shutdown();
        }
    }

    @Override
    public ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent) {
        validateEdgeEvent(edgeEvent);
        ListenableFuture<Void> saveFuture = edgeEventDao.saveAsync(edgeEvent);

        Futures.addCallback(saveFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                statsCounterService.ifPresent(statsCounterService -> statsCounterService.recordEvent(EdgeStatsKey.DOWNLINK_MSGS_ADDED, edgeEvent.getTenantId(), edgeEvent.getEdgeId(), 1));
                eventPublisher.publishEvent(SaveEntityEvent.builder()
                        .tenantId(edgeEvent.getTenantId())
                        .entityId(edgeEvent.getEdgeId())
                        .entity(edgeEvent)
                        .build());
            }

            @Override
            public void onFailure(@NotNull Throwable throwable) {}
        }, edgeEventExecutor);

        return saveFuture;
    }

}
