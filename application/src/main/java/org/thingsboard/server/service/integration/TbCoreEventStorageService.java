// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.IntegrationStatistics;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.List;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class TbCoreEventStorageService implements EventStorageService {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final ActorSystemContext actorSystemContext;
    private final TelemetrySubscriptionService telemetrySubscriptionService;
    private final EventService eventService;

    @Override
    public void persistLifecycleEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent lcEvent, Exception e) {
        actorSystemContext.persistLifecycleEvent(tenantId, entityId, lcEvent, e);
    }

    @Override
    public void persistStatistics(TenantId tenantId, IntegrationId id, long ts, IntegrationStatistics statistics, ComponentLifecycleEvent currentState) {
        String serviceId = serviceInfoProvider.getServiceId();

        eventService.saveAsync(StatisticsEvent.builder()
                .tenantId(tenantId)
                .entityId(id.getId())
                .serviceId(serviceInfoProvider.getServiceId())
                .messagesProcessed(statistics.getMessagesProcessed())
                .errorsOccurred(statistics.getErrorsOccurred())
                .build());

        List<TsKvEntry> statsTs = new ArrayList<>();
        statsTs.add(new BasicTsKvEntry(ts, new LongDataEntry(serviceId + "_messagesCount", statistics.getMessagesProcessed())));
        statsTs.add(new BasicTsKvEntry(ts, new LongDataEntry(serviceId + "_errorsCount", statistics.getErrorsOccurred())));
        statsTs.add(new BasicTsKvEntry(ts, new StringDataEntry(serviceId + "_state", currentState != null ? currentState.name() : "N/A")));
        telemetrySubscriptionService.saveTimeseriesInternal(TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(id)
                .entries(statsTs)
                .callback(new FutureCallback<>() {
                    @Override
                    public void onSuccess(Void result) {
                        log.trace("[{}] Persisted statistics telemetry: {}", id, statistics);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("[{}] Failed to persist statistics telemetry: {}", id, statistics, t);
                    }
                })
                .build());
    }

}
