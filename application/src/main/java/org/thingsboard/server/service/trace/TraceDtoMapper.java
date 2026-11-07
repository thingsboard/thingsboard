// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.thingsboard.server.common.data.trace.SpanAttributeDTO;
import org.thingsboard.server.common.data.trace.SpanDTO;
import org.thingsboard.server.common.data.trace.SpanEventDTO;
import org.thingsboard.server.common.data.trace.SpanLinkDTO;
import org.thingsboard.server.common.data.trace.SpanTreeNode;
import org.thingsboard.server.common.data.trace.Trace;
import org.thingsboard.server.dao.model.sql.trace.SpanAttributeEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEventEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanLinkEntity;
import org.thingsboard.server.dao.model.sql.trace.TraceProjection;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps the trace DAO entities to the API types in common/data, which cannot depend on the DAO.
 */
public final class TraceDtoMapper {

    private TraceDtoMapper() {
    }

    public static SpanDTO toSpanDto(SpanEntity entity) {
        return SpanDTO.builder()
                .spanId(entity.getSpanId())
                .traceId(entity.getTraceId())
                .parentSpanId(entity.getParentSpanId())
                .name(entity.getName())
                .kind(entity.getKind())
                .serviceName(entity.getServiceName())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .duration(entity.getDuration())
                .statusCode(entity.getStatusCode())
                .statusMessage(entity.getStatusMessage())
                .createdTime(entity.getCreatedTime())
                .build();
    }

    public static SpanAttributeDTO toSpanAttributeDto(SpanAttributeEntity entity) {
        return SpanAttributeDTO.builder()
                .traceId(entity.getTraceId())
                .spanId(entity.getSpanId())
                .attributeKey(entity.getAttributeKey())
                .attributeValueType(entity.getAttributeValueType())
                .attributeValueString(entity.getAttributeValueString())
                .attributeValueNumber(entity.getAttributeValueNumber())
                .attributeValueBoolean(entity.getAttributeValueBoolean())
                .build();
    }

    public static SpanEventDTO toSpanEventDto(SpanEventEntity entity) {
        return SpanEventDTO.builder()
                .id(entity.getId())
                .spanId(entity.getSpanId())
                .traceId(entity.getTraceId())
                .eventName(entity.getEventName())
                .eventTime(entity.getEventTime())
                .attributesJson(entity.getAttributesJson())
                .createdTime(entity.getCreatedTime())
                .build();
    }

    public static SpanLinkDTO toSpanLinkDto(SpanLinkEntity entity) {
        return SpanLinkDTO.builder()
                .id(entity.getId())
                .spanId(entity.getSpanId())
                .traceId(entity.getTraceId())
                .linkedTraceId(entity.getLinkedTraceId())
                .linkedSpanId(entity.getLinkedSpanId())
                .attributesJson(entity.getAttributesJson())
                .createdTime(entity.getCreatedTime())
                .build();
    }

    public static SpanTreeNode toSpanTreeNode(SpanEntity entity, List<SpanAttributeEntity> attributes) {
        List<SpanAttributeDTO> attributeDtos = attributes != null
                ? attributes.stream().map(TraceDtoMapper::toSpanAttributeDto).toList()
                : List.of();
        return SpanTreeNode.builder()
                .spanId(entity.getSpanId())
                .traceId(entity.getTraceId())
                .parentSpanId(entity.getParentSpanId())
                .name(entity.getName())
                .kind(entity.getKind())
                .serviceName(entity.getServiceName())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .duration(entity.getDuration())
                .statusCode(entity.getStatusCode())
                .statusMessage(entity.getStatusMessage())
                .createdTime(entity.getCreatedTime())
                .children(new ArrayList<>())
                .attributes(new ArrayList<>(attributeDtos))
                .build();
    }

    public static Trace toTrace(TraceProjection entity, String rootQueueName) {
        return Trace.builder()
                .rootQueueId(entity.getRootQueueId())
                .queueName(rootQueueName)
                .messageType(entity.getMessageType())
                .rootRuleChainId(entity.getRootRuleChainId())
                .ruleNodeCount(entity.getRuleNodeCount())
                .hasErrors(entity.getHasErrors())
                .hasQueueTimeouts(entity.getHasQueueTimeouts())
                .traceId(entity.getTraceId())
                .serviceName(entity.getServiceName())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .duration(entity.getDuration())
                .spanCount(entity.getSpanCount())
                .errorCount(entity.getErrorCount())
                .createdTime(entity.getCreatedTime())
                .firstSpanName(entity.getFirstSpanName())
                .inQueueTime(entity.getInQueueTime())
                .totalSpanTime(entity.getTotalSpanTime())
                .build();
    }
}
