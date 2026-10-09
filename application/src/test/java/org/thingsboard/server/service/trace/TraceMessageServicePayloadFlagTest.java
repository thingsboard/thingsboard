// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting.MessagePayloadRecording;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;
import org.thingsboard.server.service.queue.TraceRetryMetadata;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A message can arrive carrying a payload flag from an earlier trace (or set by a client); only the tenant's
 * coverage setting, captured when the trace starts, may decide whether payloads are recorded.
 */
class TraceMessageServicePayloadFlagTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final List<ReadableSpan> endedSpans = new CopyOnWriteArrayList<>();
    private final TraceCoverageSettingService coverageSettingService = Mockito.mock(TraceCoverageSettingService.class);

    private SdkTracerProvider tracerProvider;
    private TraceMessageService service;

    @BeforeEach
    void setUp() {
        tracerProvider = SdkTracerProvider.builder().addSpanProcessor(new SpanProcessor() {
            @Override
            public void onStart(Context parentContext, ReadWriteSpan span) {
            }

            @Override
            public boolean isStartRequired() {
                return false;
            }

            @Override
            public void onEnd(ReadableSpan span) {
                endedSpans.add(span);
            }

            @Override
            public boolean isEndRequired() {
                return true;
            }
        }).build();
        TraceExportPressureGate pressureGate = Mockito.mock(TraceExportPressureGate.class);
        Mockito.when(pressureGate.canCreateSpan()).thenReturn(true);
        StatsFactory statsFactory = Mockito.mock(StatsFactory.class, Mockito.RETURNS_DEEP_STUBS);
        service = new TraceMessageService(Mockito.mock(RateLimitService.class), tracerProvider.get("test"),
                Mockito.mock(PartitionService.class), Mockito.mock(TbServiceInfoProvider.class),
                coverageSettingService, pressureGate, statsFactory, 1);
        givenRecording(MessagePayloadRecording.NONE);
    }

    @AfterEach
    void tearDown() {
        tracerProvider.close();
    }

    @Test
    void givenStaleFlagAndNoneRecording_whenSelectedForCoverage_thenFlagIsRemoved() {
        TbMsg msg = msgWithStaleFlag();

        service.onForwardToRuleEngineActor(tenantId, selectedPack(msg), msg);

        assertThat(msg.isTraced()).isTrue();
        assertThat(msg.getMetaData().getValue(DataConstants.TRACE_RECORD_MSG_PAYLOAD)).isNull();
        assertThat(endedSpans).isEmpty();
    }

    @Test
    void givenStaleFlagAndFirstSpanRecording_whenSelectedForCoverage_thenFlagIsRemovedAndSnapshotIsRecorded() {
        givenRecording(MessagePayloadRecording.FIRST_SPAN);
        TbMsg msg = msgWithStaleFlag();

        service.onForwardToRuleEngineActor(tenantId, selectedPack(msg), msg);

        assertThat(msg.getMetaData().getValue(DataConstants.TRACE_RECORD_MSG_PAYLOAD)).isNull();
        assertThat(endedSpans).hasSize(1);
        assertThat(endedSpans.get(0).getName()).isEqualTo("Trace Message Snapshot");
    }

    @Test
    void givenAllSpansRecording_whenSelectedForCoverage_thenFlagIsSet() {
        givenRecording(MessagePayloadRecording.ALL_SPANS);
        TbMsg msg = msgWithStaleFlag();

        service.onForwardToRuleEngineActor(tenantId, selectedPack(msg), msg);

        assertThat(msg.getMetaData().getValue(DataConstants.TRACE_RECORD_MSG_PAYLOAD)).isEqualTo("true");
    }

    @Test
    void givenRetryWithoutFlag_whenRedeliveredMessageCarriesFlag_thenFlagIsRemoved() {
        TbMsg msg = msgWithStaleFlag();
        TbMsgPackProcessingContext packCtx = pack();
        packCtx.getMsgIdAndTraceIdMap().put(msg.getId(), TraceRetryMetadata.builder()
                .tenantId(tenantId.getId().toString())
                .traceId("0af7651916cd43dd8448eb211c80319c")
                .traceparent("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01")
                .spanOnRetry("b7ad6b7169203331")
                .build());

        service.onForwardToRuleEngineActor(tenantId, packCtx, msg);

        assertThat(msg.isTraceAuthorized()).isTrue();
        assertThat(msg.getMetaData().getValue(DataConstants.TRACE_RECORD_MSG_PAYLOAD)).isNull();
    }

    private void givenRecording(MessagePayloadRecording recording) {
        TraceCoverageSetting setting = TraceCoverageSetting.defaultSetting(tenantId.getId());
        setting.setMessagePayloadRecording(recording);
        Mockito.when(coverageSettingService.findTraceCoverageSettingByTenantId(tenantId)).thenReturn(setting);
    }

    private TbMsg msgWithStaleFlag() {
        return TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(new DeviceId(UUID.randomUUID()))
                .copyMetaData(new TbMsgMetaData(Map.of(DataConstants.TRACE_RECORD_MSG_PAYLOAD, "true")))
                .data("{\"temperature\":42}")
                .build();
    }

    private TbMsgPackProcessingContext selectedPack(TbMsg msg) {
        TbMsgPackProcessingContext packCtx = pack();
        packCtx.setTraceCoverageSelectedMsgIds(Set.of(msg.getId()));
        return packCtx;
    }

    private static TbMsgPackProcessingContext pack() {
        TbRuleEngineSubmitStrategy submitStrategy = Mockito.mock(TbRuleEngineSubmitStrategy.class);
        Mockito.when(submitStrategy.getPendingMap()).thenReturn(new ConcurrentHashMap<>());
        TbMsgPackProcessingContext packCtx = new TbMsgPackProcessingContext("Main", submitStrategy, false);
        packCtx.setMsgIdAndTraceIdMap(new ConcurrentHashMap<>());
        return packCtx;
    }

}
