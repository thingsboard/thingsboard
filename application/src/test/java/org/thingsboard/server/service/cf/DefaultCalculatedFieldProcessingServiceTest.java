// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.cf;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesImmediateOutputStrategy;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultCalculatedFieldProcessingServiceTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.fromString("e7f46b23-0c7d-42f5-9b06-fc35ab17af8a"));
    private final DeviceId deviceId = new DeviceId(UUID.fromString("ccd71696-0586-422d-940e-755a41ec3b0d"));

    @Mock
    private AttributesService attributesService;
    @Mock
    private TimeseriesService timeseriesService;
    @Mock
    private ApiLimitService apiLimitService;
    @Mock
    private RelationService relationService;
    @Mock
    private OwnersCacheService ownerService;
    @Mock
    private TbClusterService clusterService;
    @Mock
    private TelemetrySubscriptionService tsSubService;
    @Mock
    private PartitionService partitionService;

    private DefaultCalculatedFieldProcessingService service;

    @BeforeEach
    public void setUp() {
        service = new DefaultCalculatedFieldProcessingService(attributesService, timeseriesService, apiLimitService,
                relationService, ownerService, clusterService, tsSubService, partitionService);
    }

    @Test
    public void givenTimeSeriesOutputWithoutTtl_whenProcessResult_thenTenantProfileDefaultStorageTtlIsUsed() {
        when(apiLimitService.getLimit(eq(tenantId), any())).thenReturn(30L);

        service.processResult(tenantId, deviceId, "cf", buildTimeSeriesResult(0L), Collections.emptyList(), TbCallback.EMPTY);

        assertThat(captureSaveRequest().getTtl()).isEqualTo(TimeUnit.DAYS.toSeconds(30));
    }

    @Test
    public void givenTimeSeriesOutputWithCustomTtl_whenProcessResult_thenCustomTtlIsUsed() {
        service.processResult(tenantId, deviceId, "cf", buildTimeSeriesResult(3600L), Collections.emptyList(), TbCallback.EMPTY);

        assertThat(captureSaveRequest().getTtl()).isEqualTo(3600L);
        verify(apiLimitService, never()).getLimit(any(), any());
    }

    @Test
    public void givenReprocessingResultWithoutTtl_whenSave_thenTenantProfileDefaultStorageTtlIsUsed() {
        when(apiLimitService.getLimit(eq(tenantId), any())).thenReturn(30L);

        service.saveReprocessingTimeSeriesResult(tenantId, deviceId, JsonParser.parseString("{\"result\":42}"), 0L,
                System.currentTimeMillis(), TimeseriesSaveRequest.Strategy.TIME_SERIES_ONLY, TbCallback.EMPTY);

        assertThat(captureSaveRequest().getTtl()).isEqualTo(TimeUnit.DAYS.toSeconds(30));
    }

    @Test
    public void givenReprocessingResultWithCustomTtl_whenSave_thenCustomTtlIsUsed() {
        service.saveReprocessingTimeSeriesResult(tenantId, deviceId, JsonParser.parseString("{\"result\":42}"), 3600L,
                System.currentTimeMillis(), TimeseriesSaveRequest.Strategy.TIME_SERIES_ONLY, TbCallback.EMPTY);

        assertThat(captureSaveRequest().getTtl()).isEqualTo(3600L);
        verify(apiLimitService, never()).getLimit(any(), any());
    }

    private TelemetryCalculatedFieldResult buildTimeSeriesResult(long ttl) {
        return TelemetryCalculatedFieldResult.builder()
                .type(OutputType.TIME_SERIES)
                .outputStrategy(new TimeSeriesImmediateOutputStrategy(ttl, true, true, true, true))
                .result(JacksonUtil.newObjectNode().put("result", 42))
                .build();
    }

    private TimeseriesSaveRequest captureSaveRequest() {
        ArgumentCaptor<TimeseriesSaveRequest> captor = ArgumentCaptor.forClass(TimeseriesSaveRequest.class);
        verify(tsSubService).saveTimeseries(captor.capture());
        return captor.getValue();
    }

}
