package org.thingsboard.server.service.trace;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.ruleenginemonitoring.QueueCacheService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.QUEUE_ID;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.QUEUE_NAME;
import static org.thingsboard.server.common.data.trace.TraceSpanAttributes.TENANT_ID;

@Slf4j
@Component
@ConditionalOnExpression(TraceConditions.TRACE_ENABLED)
public class TraceSpanMapper {
    private static final AttributeKey<String> TENANT_ID_KEY = AttributeKey.stringKey(TENANT_ID);
    private static final AttributeKey<String> QUEUE_NAME_KEY = AttributeKey.stringKey(QUEUE_NAME);
    private static final String EXCEPTION_STACKTRACE_ATTRIBUTE = "exception.stacktrace";

    private final QueueCacheService queueCacheService;
    private final String serviceId;

    public TraceSpanMapper(QueueCacheService queueCacheService, TbServiceInfoProvider serviceInfoProvider) {
        this.queueCacheService = queueCacheService;
        this.serviceId = serviceInfoProvider.getServiceId();
    }

    public BufferedSpan toBufferedSpan(SpanData spanData, long exportTimeMs) {
        String tenantIdStr = spanData.getAttributes().get(TENANT_ID_KEY);
        if (tenantIdStr == null) {
            return null;
        }
        return new BufferedSpan(
                tenantIdStr,
                spanData.getSpanContext().getTraceId(),
                spanData.getSpanContext().getSpanId(),
                spanData.getParentSpanContext().isValid() ? spanData.getParentSpanContext().getSpanId() : null,
                spanData.getName(),
                serviceId,
                serializeSpanAttributesJson(spanData.getAttributes(), tenantIdStr),
                serializeSpanEventsJson(spanData.getEvents(), exportTimeMs),
                spanData.getStartEpochNanos() / 1_000_000,
                spanData.getEndEpochNanos() > 0 ? spanData.getEndEpochNanos() / 1_000_000 : null,
                spanData.getStatus().getStatusCode().name(),
                spanData.getStatus().getDescription(),
                exportTimeMs
        );
    }

    private JsonNode serializeSpanAttributesJson(Attributes attributes, String tenantIdStr) {
        Map<String, Object> payload = toAttributeMap(attributes);

        String queueName = attributes.get(QUEUE_NAME_KEY);
        if (tenantIdStr != null && queueName != null) {
            try {
                UUID queueId = queueCacheService.getQueueId(UUID.fromString(tenantIdStr), queueName);
                if (queueId != null) {
                    payload.put(QUEUE_ID, queueId.toString());
                }
            } catch (Exception e) {
                log.debug("Failed to resolve queue id for tenant [{}] queue [{}]", tenantIdStr, queueName, e);
            }
        }
        return JacksonUtil.valueToTree(payload);
    }

    private JsonNode serializeSpanEventsJson(List<EventData> events, long currentTime) {
        List<Map<String, Object>> payload = new ArrayList<>(events.size());
        for (EventData event : events) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", UUID.randomUUID().toString());
            item.put("eventName", event.getName());
            item.put("eventTime", event.getEpochNanos() / 1_000_000);
            item.put("createdTime", currentTime);
            Map<String, Object> attributes = toExceptionStacktraceAttributeMap(event.getAttributes());
            if (!attributes.isEmpty()) {
                item.put("attributes", attributes);
            }
            payload.add(item);
        }
        return JacksonUtil.valueToTree(payload);
    }

    private Map<String, Object> toExceptionStacktraceAttributeMap(Attributes attributes) {
        String stacktrace = attributes.get(AttributeKey.stringKey(EXCEPTION_STACKTRACE_ATTRIBUTE));
        if (stacktrace == null) {
            return Map.of();
        }
        return Map.of(EXCEPTION_STACKTRACE_ATTRIBUTE, stacktrace);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toAttributeMap(Attributes attributes) {
        Map<String, Object> attrMap = new HashMap<>();
        attributes.forEach((key, value) -> {
            AttributeType type = key.getType();
            Object serializedValue = switch (type) {
                case STRING -> attributes.get((AttributeKey<String>) key);
                case LONG -> attributes.get((AttributeKey<Long>) key);
                case DOUBLE -> attributes.get((AttributeKey<Double>) key);
                case BOOLEAN -> attributes.get((AttributeKey<Boolean>) key);
                case STRING_ARRAY -> attributes.get((AttributeKey<List<String>>) key);
                case LONG_ARRAY -> attributes.get((AttributeKey<List<Long>>) key);
                case DOUBLE_ARRAY -> attributes.get((AttributeKey<List<Double>>) key);
                case BOOLEAN_ARRAY -> attributes.get((AttributeKey<List<Boolean>>) key);
                default -> value;
            };
            attrMap.put(key.getKey(), serializedValue);
        });
        return attrMap;
    }
}
