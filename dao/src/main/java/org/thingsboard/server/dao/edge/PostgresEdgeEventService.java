// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
