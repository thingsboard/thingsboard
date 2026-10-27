// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thingsboard.server.actors.ruleChain.DefaultTbContext;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting.MessagePayloadRecording;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;
import org.thingsboard.server.service.queue.TraceRetryMetadata;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class TraceMessageServiceTest {

    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    private static final String PARENT_SPAN_ID = "b7ad6b7169203331";
    private static final String TRACE_PARENT = "00-" + TRACE_ID + "-" + PARENT_SPAN_ID + "-01";

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
                coverageSettingService, pressureGate, statsFactory, 1);
        givenRecording(MessagePayloadRecording.NONE);
    }

    @AfterEach
    void tearDown() {
        tracerProvider.close();
    }

    // A message can arrive carrying a payload flag from an earlier trace (or set by a client); only the tenant's
    // coverage setting, captured when the trace starts, may decide whether payloads are recorded.
    @Test
    void givenStaleFlagAndNoneRecording_whenSelectedForCoverage_thenFlagIsRemoved() {
        TbMsg msg = msgWithStaleFlag();

        service.onForwardToRuleEngineActor(tenantId, selectedPack(msg), msg);

        assertThat(msg.isTraced()).isTrue();
        assertThat(msg.isRecordMsgPayload()).isFalse();
        assertThat(endedSpans).isEmpty();
    }

    @Test
    void givenStaleFlagAndFirstSpanRecording_whenSelectedForCoverage_thenFlagIsRemovedAndSnapshotIsRecorded() {
        givenRecording(MessagePayloadRecording.FIRST_SPAN);
        TbMsg msg = msgWithStaleFlag();

        service.onForwardToRuleEngineActor(tenantId, selectedPack(msg), msg);

        assertThat(msg.isRecordMsgPayload()).isFalse();
        assertThat(endedSpans).hasSize(1);
        assertThat(endedSpans.get(0).getName()).isEqualTo("Trace Message Snapshot");
    }

    @Test
    void givenAllSpansRecording_whenSelectedForCoverage_thenFlagIsSet() {
        givenRecording(MessagePayloadRecording.ALL_SPANS);
        TbMsg msg = msgWithStaleFlag();

        service.onForwardToRuleEngineActor(tenantId, selectedPack(msg), msg);

        assertThat(msg.isRecordMsgPayload()).isTrue();
    }

    @Test
    void givenRetryWithoutFlag_whenRedeliveredMessageCarriesFlag_thenFlagIsRemoved() {
        TbMsg msg = msgWithStaleFlag();
        TbMsgPackProcessingContext packCtx = pack();
        packCtx.getMsgIdAndTraceIdMap().put(msg.getId(), TraceRetryMetadata.builder()
                .tenantId(tenantId.getId().toString())
                .traceId(TRACE_ID)
                .traceparent(TRACE_PARENT)
                .spanOnRetry(PARENT_SPAN_ID)
                .build());

        service.onForwardToRuleEngineActor(tenantId, packCtx, msg);

        assertThat(msg.isTraceAuthorized()).isTrue();
        assertThat(msg.isRecordMsgPayload()).isFalse();
    }

    @Test
    void givenSpanAttributesFail_whenRuleNodeEnter_thenSpanEndsAsErrorAndTraceStateIsCleared() {
        TbMsg msg = tracedMsg();
        RuleNode ruleNode = ruleNode();
        // No rule chain id: addSpanAttributes throws after the span has started.
        ruleNode.setRuleChainId(null);
        DefaultTbContext tbContext = ruleNodeContext(ruleNode);

        service.onRuleNodeEnter(tbContext, msg);

        assertThat(endedSpans).hasSize(1);
        assertThat(endedSpans.get(0).toSpanData().getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        Mockito.verify(tbContext).setProcessingMsgSpan(null);
        Mockito.verify(tbContext, Mockito.never()).setProcessingMsgSpan(Mockito.argThat(span -> span != null));
        assertThat(msg.isTraced()).isFalse();
        assertThat(msg.isTraceAuthorized()).isFalse();
        assertThat(msg.getTraceParent()).isNull();
    }

    @Test
    void givenTracedMsg_whenMaxRuleNodeExecutionsOnFreshContext_thenErrorSpanIsExported() {
        TbMsg msg = tracedMsg();
        DefaultTbContext tbContext = ruleNodeContext(ruleNode());

        service.onMaxRuleNodeExecutions(tbContext, msg, 50);

        assertThat(endedSpans).hasSize(1);
        SpanData span = endedSpans.get(0).toSpanData();
        assertThat(span.getName()).isEqualTo("[Root Rule Chain] [Save Timeseries]");
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(span.getStatus().getDescription()).isEqualTo("Max rule node executions exceeded");
        assertThat(span.getAttributes().get(AttributeKey.longKey("rule.node.max.executions"))).isEqualTo(50L);
        assertThat(span.getParentSpanId()).isEqualTo(PARENT_SPAN_ID);
        assertThat(msg.getTraceParent()).isEqualTo(TRACE_PARENT);
    }

    @Test
    void givenUntracedMsg_whenMaxRuleNodeExecutions_thenNoSpan() {
        service.onMaxRuleNodeExecutions(ruleNodeContext(ruleNode()), msgWithStaleFlag(), 50);

        assertThat(endedSpans).isEmpty();
    }

    private TbMsg tracedMsg() {
        TbMsg msg = msgWithStaleFlag();
        msg.setTraceId(TRACE_ID);
        msg.setTraceParent(TRACE_PARENT);
        msg.setTraceAuthorized(true);
        return msg;
    }

    private static RuleNode ruleNode() {
        RuleNode ruleNode = new RuleNode(new RuleNodeId(UUID.randomUUID()));
        ruleNode.setRuleChainId(new RuleChainId(UUID.randomUUID()));
        ruleNode.setName("Save Timeseries");
        return ruleNode;
    }

    private DefaultTbContext ruleNodeContext(RuleNode ruleNode) {
        DefaultTbContext tbContext = Mockito.mock(DefaultTbContext.class);
        Mockito.when(tbContext.getTenantId()).thenReturn(tenantId);
        Mockito.when(tbContext.getRuleChainName()).thenReturn("Root Rule Chain");
        Mockito.when(tbContext.getSelf()).thenReturn(ruleNode);
        return tbContext;
    }

    private void givenRecording(MessagePayloadRecording recording) {
        TraceCoverageSetting setting = TraceCoverageSetting.defaultSetting(tenantId.getId());
        setting.setMessagePayloadRecording(recording);
        Mockito.when(coverageSettingService.findTraceCoverageSettingByTenantId(tenantId)).thenReturn(setting);
    }

    private TbMsg msgWithStaleFlag() {
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(new DeviceId(UUID.randomUUID()))
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data("{\"temperature\":42}")
                .build();
        msg.setRecordMsgPayload(true);
        return msg;
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
