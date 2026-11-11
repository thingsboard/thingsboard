// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.List;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AgentMetricsTimeseriesService {

    @Value("${agents.metrics.ttl:1800}")
    private long metricsTtlSeconds;

    private final TelemetrySubscriptionService tsSubService;

    public void save(TenantId tenantId, EntityId entityId, List<TsKvEntry> kvEntries, String entityIdentifier) {
        tsSubService.saveTimeseries(TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(entityId)
                .entries(kvEntries)
                .ttl(metricsTtlSeconds)
                .strategy(TimeseriesSaveRequest.Strategy.PROCESS_ALL)
                .callback(getMetricSaveCallback(tenantId, entityIdentifier))
                .build());
    }

    private FutureCallback<Void> getMetricSaveCallback(TenantId tenantId, String identifier) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                log.trace("[{}] Saved metric timeseries for {}", tenantId, identifier);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to save metric timeseries for {}", tenantId, identifier, t);
            }
        };
    }
}
