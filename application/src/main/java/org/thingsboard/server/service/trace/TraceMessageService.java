/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.trace;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.Tracer;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.ruleChain.DefaultTbContext;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;
import org.thingsboard.server.service.queue.TraceRetryMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
@Service
@ConditionalOnExpression(TraceConditions.TRACE_ENABLED)
public class TraceMessageService {
    private static final String TRACE_MESSAGE_SNAPSHOT_SPAN = "Trace Message Snapshot";

    private enum TraceIngressType {
        UNTRACED,
        RETRY_RESTORED,
        INTERNAL_AUTHORIZED,
        TRACED_RATE_LIMIT_PENDING
    }

    private final RateLimitService rateLimitService;
    private final Tracer tracer;
    private final PartitionService partitionService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TraceCoverageSettingService traceCoverageSettingService;
    private final TraceExportPressureGate pressureGate;

    private final Cache<UUID, TraceCoverageSetting> coverageSettingCache;

    private final Timer onMessagePackTimer;

    public TraceMessageService(RateLimitService rateLimitService, Tracer tracer,
                               PartitionService partitionService, TbServiceInfoProvider serviceInfoProvider,
                               TraceCoverageSettingService traceCoverageSettingService,
                               TraceExportPressureGate pressureGate,
                               StatsFactory statsFactory,
                               @Value("${otel.tracing.coverage.settings-cache-ttl-minutes:1}") int settingsCacheTtlMinutes) {
        this.rateLimitService = rateLimitService;
        this.tracer = tracer;
        this.partitionService = partitionService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.traceCoverageSettingService = traceCoverageSettingService;
        this.pressureGate = pressureGate;
        this.coverageSettingCache = Caffeine.newBuilder()
                .expireAfterWrite(settingsCacheTtlMinutes, TimeUnit.MINUTES)
                .build();
        this.onMessagePackTimer = statsFactory.createTimer("traceCoverageOnMessagePackDuration");
    }

    public void onForwardToRuleEngineActor(TenantId tenantId, TbMsgPackProcessingContext packCtx, TbMsg tbMsg) {
        try {
            switch (classifyIngress(packCtx, tbMsg)) {
                case UNTRACED:
                    handleUntracedMessage(tenantId, packCtx, tbMsg);
                    break;
                case RETRY_RESTORED:
                    restoreTraceMetadataFromRetryContext(packCtx, tbMsg);
                    break;
                case INTERNAL_AUTHORIZED:
                    registerRetryMetadata(tenantId, packCtx, tbMsg);
                    break;
                case TRACED_RATE_LIMIT_PENDING:

                    break;
            }
        } catch (Exception e) {
            log.warn("[{}] Failed to set trace context", tenantId, e);
        }
    }

    public void onRuleNodeEnter(DefaultTbContext tbContext, TbMsg tbMsg) {
        Span span = null;
        try {
            span = startRuleNodeSpan(tbContext, tbMsg);
            if (span == null) {
                return;
            }
            String newTraceParent = TraceUtils.buildTraceparent(span.getSpanContext());
            tbMsg.getMetaData().putValue(DataConstants.TRACE_PARENT, newTraceParent);
            tbContext.setProcessingMsg(tbMsg);
            tbContext.setProcessingMsgSpan(span);
        } catch (Exception e) {
            log.warn("[{}] Failed to evaluate traceable message", tbContext.getTenantId(), e);
            // Don't leave a half-built span on the context or trace keys on the message that point to it.
            if (span != null) {
                endWithError(span, e);
            }
            tbContext.setProcessingMsgSpan(null);
            stripTraceState(tbMsg);
        }
    }

    public void onMaxRuleNodeExecutions(DefaultTbContext tbContext, TbMsg tbMsg, int maxRuleNodeExecutions) {
        try {
            // The node never runs, so this span is the only record that the trace stopped here.
            Span span = startRuleNodeSpan(tbContext, tbMsg);
            if (span == null) {
                return;
            }
            span.setAttribute("rule.node.max.executions", maxRuleNodeExecutions);
            span.setStatus(StatusCode.ERROR, "Max rule node executions exceeded");
            span.end();
        } catch (Exception e) {
            log.warn("[{}] Failed to handle maxRuleNodeExecutionsError trace", tbContext.getTenantId(), e);
        }
    }

    /**
     * Starts the span of the rule node handling {@code tbMsg}, or returns null when the message isn't traceable
     * (stripping its trace state where the trace can't continue). The caller ends the span.
     */
    private Span startRuleNodeSpan(DefaultTbContext tbContext, TbMsg tbMsg) {
        if (tbMsg.getTraceParent() == null || !tbMsg.isTraceAuthorized()) {
            return null;
        }
        TenantId tenantId = tbContext.getTenantId();
        SpanContext parentSpanContext = resolveParentSpanContext(tbMsg);
        if (parentSpanContext == null || !parentSpanContext.isValid() || !parentSpanContext.isSampled()) {
            stripTraceState(tbMsg);
            log.debug("[{}] Failed to set trace context", tenantId);
            return null;
        }
        String traceId = tbMsg.getTraceId();
        if (!pressureGate.canCreateSpan()) {
            stripTraceState(tbMsg);
            log.debug("[{}] Trace [{}] span dropped, export queue is full", tenantId, traceId);
            return null;
        }

        String ruleChainName = tbContext.getRuleChainName();
        RuleNode ruleNode = tbContext.getSelf();
        Span span = tracer.spanBuilder(TraceUtils.buildSpanName(ruleChainName, ruleNode))
                .setParent(Context.current().with(Span.wrap(parentSpanContext)))
                .startSpan();
        if (!span.isRecording()) {
            stripTraceState(tbMsg);
            log.debug("[{}] Trace [{}] span is not recording", tenantId, traceId);
            return null;
        }
        try {
            addSpanAttributes(span, tbMsg, ruleChainName, ruleNode, tenantId, TraceUtils.shouldRecordMsgPayload(tbMsg.getMetaData()));
        } catch (RuntimeException e) {
            endWithError(span, e);
            throw e;
        }
        return span;
    }

    private static void endWithError(Span span, Exception e) {
        span.setStatus(StatusCode.ERROR, "Trace instrumentation failed: " + e.getMessage());
        span.end();
    }

    public void onPutToQueue(TbMsg sourceMsg, TbMsg childMsg) {
        try {
            if (!sourceMsg.isTraced() || !childMsg.isTraced()) {
                return;
            }
            propagateTraceMetadata(sourceMsg, childMsg, null);
        } catch (Exception e) {
            log.warn("Failed to handle putToQueue trace", e);
        }
    }

    public void onTimeout(TbMsgPackProcessingContext packCtx) {
        try {
            packCtx.getPendingMap().forEach((strategyMsgId, protoMsg) -> {
                MsgProtos.TbMsgProto tbMsgProto = null;
                try {
                    tbMsgProto = ProtoUtils.getTbMsgProto(protoMsg.getValue());
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
                TraceRetryMetadata traceMetadata = packCtx.getTraceMetadata(UUID.fromString(tbMsgProto.getId()));
                if (traceMetadata == null) return;
                SpanContext parentSpanContext = TraceUtils.parseRemoteParentSpanContext(traceMetadata.getTraceparent());

                Span span = tracer.spanBuilder("Queue Message Processing Time Out")
                        .setParent(Context.current().with(Span.wrap(parentSpanContext)))
                        .setAttribute("message.id", tbMsgProto.getId())
                        .setAttribute("tenant.id", traceMetadata.getTenantId())
                        .setAttribute("queue.name", packCtx.getQueueName())
                        .setAttribute("queue.cancel.time.out.messages", packCtx.isSkipTimeoutMsgsPossible())
                        .startSpan();
                span.setStatus(StatusCode.OK);
                span.end();
            });
        } catch (Exception e) {
            log.error("Failed to handle onTimeout", e);
        }
    }

    public void onTellSuccess(DefaultTbContext tbContext, TbMsg msg) {
        try {
            if (tbContext.getProcessingMsgSpan() == null) {
                return;
            }
            if (isProcessingMsg(tbContext, msg)) {
                Span span = tbContext.getProcessingMsgSpan();
                if (span != null) {
                    addOutputMsgAttributes(span, msg, TraceUtils.shouldRecordMsgPayload(msg.getMetaData()));
                    span.setStatus(StatusCode.OK);
                    span.end();
                }
            } else {
                propagateTraceMetadata(tbContext.getProcessingMsg(), msg, tbContext.getProcessingMsgSpan());
            }
        } catch (Exception e) {
            log.warn("Failed to handle endSpanSuccess trace", e);
        }
    }

    public void onTellFailure(DefaultTbContext tbContext, TbMsg msg, Throwable th) {
        try {
            if (tbContext.getProcessingMsgSpan() == null) {
                return;
            }
            if (isProcessingMsg(tbContext, msg)) {
                Span span = tbContext.getProcessingMsgSpan();
                if (span != null) {
                    addOutputMsgAttributes(span, msg, TraceUtils.shouldRecordMsgPayload(msg.getMetaData()));
                    span.setStatus(StatusCode.ERROR, errorStatusMessage(th));
                    span.end();
                }
            } else {
                propagateTraceMetadata(tbContext.getProcessingMsg(), msg, tbContext.getProcessingMsgSpan());
            }
        } catch (Exception e) {
            log.warn("Failed to handle endSpanFailure trace", e);
        }
    }

    public void onAck(DefaultTbContext tbContext, TbMsg tbMsg) {
        try {
            if (tbContext.getProcessingMsgSpan() == null) {
                return;
            }
            if (!isProcessingMsg(tbContext, tbMsg)) {
                return;
            }
            Span span = tbContext.getProcessingMsgSpan();
            if (span != null) {
                addOutputMsgAttributes(span, tbMsg, TraceUtils.shouldRecordMsgPayload(tbMsg.getMetaData()));
                span.setStatus(StatusCode.OK);
                span.end();
            }
        } catch (Exception e) {
            log.warn("Failed to handle ack trace", e);
        }
    }

    public void onMessagePack(TenantId queueTenantId, TbMsgPackProcessingContext packCtx) {
        long startTs = System.currentTimeMillis();
        try {
            if (packCtx.isHasRetry()) {
                return;
            }

            Map<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pending = packCtx.getPendingMap();
            if (pending == null || pending.isEmpty()) {
                return;
            }
            boolean systemQueue = queueTenantId.isSysTenantId();

            Set<TenantId> tenants = collectUntracedByTenant(queueTenantId, systemQueue, pending,
                    resolvePackTenants(queueTenantId, systemQueue, pending), packCtx).keySet();
            if (tenants.isEmpty()) {
                return;
            }

            Map<TenantId, TraceCoverageSetting> tenantsToSample = resolveTenantsToSample(tenants);
            if (tenantsToSample.isEmpty()) {
                return;
            }

            Set<UUID> selected = selectCoveredMessages(queueTenantId, systemQueue, pending, packCtx, tenantsToSample);
            if (!selected.isEmpty()) {
                packCtx.setTraceCoverageSelectedMsgIds(selected);
            }
        } catch (Exception e) {
            log.warn("[{}] Failed to evaluate trace coverage for message pack", queueTenantId, e);
        } finally {
            onMessagePackTimer.record(System.currentTimeMillis() - startTs, MILLISECONDS);
        }
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onComponentLifecycleEvent(ComponentLifecycleMsg event) {
        if (event.getEntityId() != null && TraceCoverageSetting.SETTINGS_ASSET_ID.equals(event.getEntityId().getId())) {
            coverageSettingCache.invalidateAll();
            log.debug("Invalidated trace coverage setting cache on lifecycle event {}", event.getEvent());
        }
    }

    public void onRuleChainOutput(DefaultTbContext tbContext, TbMsg tbMsg) {
        try {
            if (!tbMsg.isTraced()) return;
            Span span = tbContext.getProcessingMsgSpan();
            if (span != null && isProcessingMsg(tbContext, tbMsg)) {
                addOutputMsgAttributes(span, tbMsg, TraceUtils.shouldRecordMsgPayload(tbMsg.getMetaData()));
                span.setStatus(StatusCode.OK);
                span.end();
            }
        } catch (Exception e) {
            log.warn("Failed to handle ruleChainOutput trace", e);
        }
    }

    public void onEnqueueForTellNext(DefaultTbContext tbContext, TbMsg newMsg) {
        try {
            if (tbContext.getProcessingMsgSpan() == null) {
                return;
            }
            propagateTraceMetadata(tbContext.getProcessingMsg(), newMsg, tbContext.getProcessingMsgSpan());
        } catch (Exception e) {
            log.warn("Failed to handle enqueueForTellNext trace", e);
        }
    }

    public void onEnqueue(DefaultTbContext tbContext, TbMsg tbMsg) {
        try {
            if (tbContext.getProcessingMsgSpan() == null) {
                return;
            }
            propagateTraceMetadata(tbContext.getProcessingMsg(), tbMsg, tbContext.getProcessingMsgSpan());
        } catch (Exception e) {
            log.warn("Failed to handle enqueue trace", e);
        }
    }

    private TraceIngressType classifyIngress(TbMsgPackProcessingContext packCtx, TbMsg tbMsg) {
        if (hasRetryMetadata(packCtx, tbMsg.getId())) {
            return TraceIngressType.RETRY_RESTORED;
        }

        if (tbMsg.isTraceAuthorized()) {
            return TraceIngressType.INTERNAL_AUTHORIZED;
        }

        return TraceIngressType.UNTRACED;
    }

    private boolean hasRetryMetadata(TbMsgPackProcessingContext packCtx, UUID msgId) {
        return packCtx != null && packCtx.getTraceMetadata(msgId) != null;
    }

    private void handlePendingTraceAuthorization(TenantId tenantId, TbMsgPackProcessingContext packCtx, TbMsg tbMsg) {
        authorizeTraceInjection(tenantId, packCtx, tbMsg, "Trace injection");
    }

    private boolean authorizeTraceInjection(TenantId tenantId, TbMsgPackProcessingContext packCtx, TbMsg tbMsg, String reason) {
        String traceparent = TraceUtils.getTraceParent(tbMsg.getMetaData());
        if (!TraceUtils.isValidTraceparent(traceparent) || TraceUtils.isUnsampledTraceparent(traceparent)) {
            log.debug("[{}] {} missing or invalid traceparent, stripping trace metadata", tenantId, reason);
            stripTraceState(tbMsg);
            return false;
        }
        tbMsg.getMetaData().putValue(DataConstants.TRACE_ID, TraceUtils.extractTraceIdFromTraceparent(traceparent));
        tbMsg.getMetaData().putValue(DataConstants.SPAN_ON_RETRY, TraceUtils.extractSpanIdFromTraceparent(traceparent));

        tbMsg.setTraceAuthorized(true);

        registerRetryMetadata(tenantId, packCtx, tbMsg);
        return true;
    }

    private void handleUntracedMessage(TenantId tenantId, TbMsgPackProcessingContext packCtx, TbMsg tbMsg) {
        if (packCtx == null || !packCtx.isTraceCoverageSelected(tbMsg.getId())) {
            return;
        }
        if (hasRetryMetadata(packCtx, tbMsg.getId())) {
            return;
        }

        String traceId = IdGenerator.random().generateTraceId();
        String rootSpanId = IdGenerator.random().generateSpanId();

        TbMsgMetaData metaData = tbMsg.getMetaData();
        // Trace keys left by an earlier trace (e.g. a stale payload flag) must not leak into the new one.
        TraceUtils.stripTraceMetadata(metaData);
        metaData.putValue(DataConstants.TRACE_ID, traceId);
        metaData.putValue(DataConstants.TRACE_PARENT, TraceUtils.buildTraceparent(traceId, rootSpanId, TraceFlags.getSampled().asHex()));
        metaData.putValue(DataConstants.SPAN_ON_RETRY, rootSpanId);

        tbMsg.setTraceAuthorized(true);

        TraceCoverageSetting coverageSetting = getCoverageSetting(tenantId);
        TraceCoverageSetting.MessagePayloadRecording payloadRecording = coverageSetting.getMessagePayloadRecording() != null
                ? coverageSetting.getMessagePayloadRecording()
                : TraceCoverageSetting.DEFAULT_MESSAGE_PAYLOAD_RECORDING;
        if (payloadRecording == TraceCoverageSetting.MessagePayloadRecording.ALL_SPANS) {
            metaData.putValue(DataConstants.TRACE_RECORD_MSG_PAYLOAD, Boolean.TRUE.toString());
        }
        if (payloadRecording == TraceCoverageSetting.MessagePayloadRecording.FIRST_SPAN) {
            recordMessageSnapshotSpan(tenantId, packCtx, tbMsg);
        }

        registerRetryMetadata(tenantId, packCtx, tbMsg);
    }

    private void recordMessageSnapshotSpan(TenantId tenantId, TbMsgPackProcessingContext packCtx, TbMsg tbMsg) {
        SpanContext parentSpanContext = TraceUtils.parseRemoteParentSpanContext(TraceUtils.getTraceParent(tbMsg.getMetaData()));
        if (parentSpanContext == null || !parentSpanContext.isValid() || !parentSpanContext.isSampled()) {
            return;
        }

        if (!pressureGate.canCreateSpan()) {
            return;
        }
        Span span = tracer.spanBuilder(TRACE_MESSAGE_SNAPSHOT_SPAN)
                .setParent(Context.current().with(Span.wrap(parentSpanContext)))
                .setAttribute("message.id", tbMsg.getId().toString())
                .setAttribute("tenant.id", tenantId.getId().toString())
                .setAttribute("queue.name", packCtx != null ? packCtx.getQueueName() : tbMsg.getQueueName())
                .setAttribute("input.msg.data", tbMsg.getData())
                .setAttribute("input.msg.metadata", serializeMetadataToJson(tbMsg.getMetaData()))
                .startSpan();
        span.setStatus(StatusCode.OK);
        span.end();
    }

    private void restoreTraceMetadataFromRetryContext(TbMsgPackProcessingContext packCtx, TbMsg tbMsg) {
        if (packCtx == null) {
            return;
        }
        TraceRetryMetadata retryMetadata = packCtx.getTraceMetadata(tbMsg.getId());
        if (retryMetadata == null) {
            return;
        }
        TraceUtils.putTraceMetaIfMissing(tbMsg.getMetaData(), DataConstants.TRACE_ID, retryMetadata.getTraceId());
        TraceUtils.putTraceMetaIfMissing(tbMsg.getMetaData(), DataConstants.TRACE_PARENT, retryMetadata.getTraceparent());
        TraceUtils.putTraceMetaIfMissing(tbMsg.getMetaData(), DataConstants.SPAN_ON_RETRY, retryMetadata.getSpanOnRetry());
        // The flag decided when the trace started wins over whatever the redelivered message carries.
        if (retryMetadata.getTraceRecordMsgPayload() != null) {
            tbMsg.getMetaData().putValue(DataConstants.TRACE_RECORD_MSG_PAYLOAD, retryMetadata.getTraceRecordMsgPayload());
        } else {
            tbMsg.getMetaData().getData().remove(DataConstants.TRACE_RECORD_MSG_PAYLOAD);
        }
        tbMsg.setTraceAuthorized(true);
    }

    private static void stripTraceState(TbMsg tbMsg) {
        TraceUtils.stripTraceMetadata(tbMsg.getMetaData());
        tbMsg.setTraceAuthorized(false);
    }

    private void registerRetryMetadata(TenantId tenantId, TbMsgPackProcessingContext packCtx, TbMsg tbMsg) {
        if (packCtx == null || !tbMsg.isTraced() || !tbMsg.isTraceAuthorized()) {
            return;
        }
        packCtx.registerTraceMetadata(tbMsg.getId(), TraceRetryMetadata.builder()
                .tenantId(tenantId.getId().toString())
                .traceId(tbMsg.getTraceId())
                .traceparent(TraceUtils.getTraceParent(tbMsg.getMetaData()))
                .spanOnRetry(TraceUtils.getSpanOnRetry(tbMsg.getMetaData()))
                .traceRecordMsgPayload(tbMsg.getMetaData().getValue(DataConstants.TRACE_RECORD_MSG_PAYLOAD))
                .build());
    }

    private boolean isProcessingMsg(DefaultTbContext tbContext, TbMsg msg) {
        TbMsg processingMsg = tbContext.getProcessingMsg();
        return processingMsg != null && processingMsg.getId().equals(msg.getId());
    }

    private Set<TenantId> resolvePackTenants(TenantId queueTenantId, boolean systemQueue,
                                             Map<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pending) {
        if (!systemQueue) {
            return Collections.singleton(queueTenantId);
        }
        Set<TenantId> tenants = new HashSet<>();
        for (TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg : pending.values()) {
            tenants.add(tenantIdOf(msg.getValue()));
        }
        return tenants;
    }

    private Map<TenantId, TraceCoverageSetting> resolveTenantsToSample(Set<TenantId> tenants) {
        long nowEpochSeconds = System.currentTimeMillis() / 1000L;
        List<String> sortedServiceIds = sortedRuleEngineServiceIds();
        String selfServiceId = serviceInfoProvider.getServiceId();
        Map<TenantId, TraceCoverageSetting> tenantsToSample = new HashMap<>();
        for (TenantId tenantId : tenants) {
            TraceCoverageSetting setting = getCoverageSetting(tenantId);
            if (setting == null || !setting.isEnabled()) {
                continue;
            }
            if (setting.isRuleEngineRotation()
                    && !isActiveInstance(sortedServiceIds, selfServiceId, setting.getSwitchPeriodSeconds(), nowEpochSeconds)) {
                continue;
            }
            tenantsToSample.put(tenantId, setting);
        }
        return tenantsToSample;
    }

    private Map<TenantId, List<UUID>> collectUntracedByTenant(
            TenantId queueTenantId, boolean systemQueue,
            Map<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pending,
            Set<TenantId> tenantsToSample,
            TbMsgPackProcessingContext packCtx) {
        Map<TenantId, List<UUID>> byTenant = new HashMap<>();
        for (TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> queueMsg : pending.values()) {
            TransportProtos.ToRuleEngineMsg toRuleEngineMsg = queueMsg.getValue();
            TenantId tenantId = systemQueue ? tenantIdOf(toRuleEngineMsg) : queueTenantId;
            if (!tenantsToSample.contains(tenantId)) {
                continue;
            }
            MsgProtos.TbMsgProto proto = extractTbMsgProto(toRuleEngineMsg);
            if (proto == null) {
                continue;
            }

            if (proto.getRuleNodeIdMSB() != 0L || proto.getRuleNodeIdLSB() != 0L) {
                continue;
            }

            UUID msgId;
            try {
                msgId = UUID.fromString(proto.getId());
            } catch (Exception e) {
                continue;
            }

            if (hasRetryMetadata(packCtx, msgId)) {
                continue;
            }
            if (proto.getCtx().getTraceAuthorized()) {
                continue;
            }

            byTenant.computeIfAbsent(tenantId, t -> new ArrayList<>()).add(msgId);
        }
        return byTenant;
    }

    private Set<UUID> selectCoveredMessages(TenantId queueTenantId, boolean systemQueue,
                                            Map<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pending,
                                            TbMsgPackProcessingContext packCtx,
                                            Map<TenantId, TraceCoverageSetting> tenantsToSample) {
        Map<TenantId, List<UUID>> byTenant =
                collectUntracedByTenant(queueTenantId, systemQueue, pending, tenantsToSample.keySet(), packCtx);
        Set<UUID> selected = new HashSet<>();
        for (Map.Entry<TenantId, List<UUID>> tenantEntry : byTenant.entrySet()) {
            TenantId tenantId = tenantEntry.getKey();
            TraceCoverageSetting setting = tenantsToSample.get(tenantId);
            int maxPerPack = setting.getTracesPerPack();
            String rateLimitConfig = setting.getTracesPerInterval() + ":" + setting.getIntervalSeconds();
            List<UUID> candidates = tenantEntry.getValue();
            Collections.shuffle(candidates);
            int count = 0;
            for (UUID msgId : candidates) {
                if (maxPerPack > 0 && count >= maxPerPack) {
                    break;
                }

                if (!pressureGate.canIssueTraceId()) {
                    break;
                }
                if (!rateLimitService.checkRateLimit(LimitedApi.TRACES_PER_TENANT, tenantId.getId(), rateLimitConfig)) {
                    break;
                }
                selected.add(msgId);
                count++;
            }
        }
        return selected;
    }

    private static TenantId tenantIdOf(TransportProtos.ToRuleEngineMsg msg) {
        return TenantId.fromUUID(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
    }

    private static MsgProtos.TbMsgProto extractTbMsgProto(TransportProtos.ToRuleEngineMsg toRuleEngineMsg) {
        try {
            return toRuleEngineMsg.getTbMsg().isEmpty()
                    ? toRuleEngineMsg.getTbMsgProto()
                    : MsgProtos.TbMsgProto.parseFrom(toRuleEngineMsg.getTbMsg());
        } catch (Exception e) {
            return null;
        }
    }

    private static String errorStatusMessage(Throwable th) {
        if (th == null) {
            return "Unknown error";
        }
        String type = th.getClass().getSimpleName();
        String message = th.getMessage();
        if (message == null || message.isBlank()) {
            return type;
        }
        return type + ": " + message;
    }

    private TraceCoverageSetting getCoverageSetting(TenantId tenantId) {
        return coverageSettingCache.get(tenantId.getId(),
                id -> traceCoverageSettingService.findTraceCoverageSettingByTenantId(tenantId));
    }

    private List<String> sortedRuleEngineServiceIds() {
        Set<String> idSet = partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE);
        if (idSet == null || idSet.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>(idSet);
        Collections.sort(ids);
        return ids;
    }

    private boolean isActiveInstance(List<String> sortedServiceIds, String selfServiceId, int switchPeriodSeconds, long nowEpochSeconds) {
        if (sortedServiceIds.isEmpty()) {
            return true;
        }
        int period = Math.max(1, switchPeriodSeconds);
        long periodIndex = nowEpochSeconds / period;
        int idx = Math.floorMod(periodIndex, sortedServiceIds.size());
        return sortedServiceIds.get(idx).equals(selfServiceId);
    }

    private String serializeMetadataToJson(TbMsgMetaData metaData) {
        return JacksonUtil.writeValueAsString(metaData.values());
    }

    private void addOutputMsgAttributes(Span span, TbMsg msg, boolean recordMsgPayload) {
        if (recordMsgPayload) {
            span.setAttribute("output.msg.data", msg.getData());
            span.setAttribute("output.msg.metadata", serializeMetadataToJson(msg.getMetaData()));
        }
    }

    private void addSpanAttributes(Span span, TbMsg tbMsg,
                                   String ruleChainName, RuleNode ruleNode, TenantId tenantId, boolean recordMsgPayload) {
        span.setAttribute("message.id", tbMsg.getId().toString())
                .setAttribute("rule.chain.id", ruleNode.getRuleChainId().getId().toString())
                .setAttribute("rule.node.id", ruleNode.getId().getId().toString())
                .setAttribute("rule.chain.name", ruleChainName)
                .setAttribute("message.type", tbMsg.getType())
                .setAttribute("queue.name", tbMsg.getQueueName())
                .setAttribute("originator.id", tbMsg.getOriginator().getId().toString())
                .setAttribute("tenant.id", tenantId.getId().toString());

        if (recordMsgPayload) {
            span.setAttribute("input.msg.data", tbMsg.getData())
                    .setAttribute("input.msg.metadata", serializeMetadataToJson(tbMsg.getMetaData()));
        }
    }

    private void propagateTraceMetadata(TbMsg sourceMsg, TbMsg targetMsg, Span parentSpan) {
        if (sourceMsg == null || targetMsg == null || !sourceMsg.isTraced()) {
            return;
        }
        if (sourceMsg.isTraceAuthorized()) {
            targetMsg.setTraceAuthorized(true);
            targetMsg.getMetaData().putValue(DataConstants.TRACE_ID, sourceMsg.getTraceId());
        }
        String traceparent;
        if (parentSpan != null) {
            traceparent = TraceUtils.buildTraceparent(parentSpan.getSpanContext());
        } else {
            traceparent = TraceUtils.getTraceParent(sourceMsg.getMetaData());
        }

        if (traceparent != null) {
            targetMsg.getMetaData().putValue(DataConstants.TRACE_PARENT, traceparent);
        }
        String recordMsgPayload = sourceMsg.getMetaData().getValue(DataConstants.TRACE_RECORD_MSG_PAYLOAD);
        if (recordMsgPayload != null) {
            targetMsg.getMetaData().putValue(DataConstants.TRACE_RECORD_MSG_PAYLOAD, recordMsgPayload);
        }

        if (TraceUtils.getSpanOnRetry(targetMsg.getMetaData()) == null) {
            String retrySpanId = parentSpan != null
                    ? parentSpan.getSpanContext().getSpanId()
                    : TraceUtils.extractSpanIdFromTraceparent(traceparent);
            if (retrySpanId != null) {
                targetMsg.getMetaData().putValue(DataConstants.SPAN_ON_RETRY, retrySpanId);
            }
        }
    }

    private SpanContext resolveParentSpanContext(TbMsg tbMsg) {
        String traceId = tbMsg.getTraceId();
        if (traceId == null) {
            return null;
        }

        String traceparent = TraceUtils.getTraceParent(tbMsg.getMetaData());
        if (traceparent == null) {
            return null;
        }
        return TraceUtils.parseRemoteParentSpanContext(traceparent);
    }
}
