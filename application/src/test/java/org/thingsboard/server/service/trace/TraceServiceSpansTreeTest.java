// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trace.SpanAttributeDTO;
import org.thingsboard.server.common.data.trace.SpanTreeNode;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.sql.trace.TraceDao;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TraceServiceSpansTreeTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final String TRACE_ID = "trace-1";

    private final TraceDao traceDao = mock(TraceDao.class);
    private final TraceService service = new TraceService(traceDao, mock(RuleChainService.class), mock(QueueService.class),
            mock(RuleEngineMonitoringDao.class), mock(PartitionService.class), mock(ComponentDiscoveryService.class));

    @Test
    void givenSpansWithAttributes_whenGetSpansTree_thenAttributesComeFromTheLoadedSpansWithoutAnotherQuery() {
        when(traceDao.findSpansByTraceId(TENANT_ID.getId(), TRACE_ID)).thenReturn(List.of(
                span("root", null, 0, "{\"rule.node.id\":\"n1\",\"input.msg.data\":\"{}\"}"),
                span("child", "root", 10, "{\"rule.node.id\":\"n2\"}")));

        List<SpanTreeNode> tree = service.getSpansTree(TENANT_ID, new TraceSpanListQuery(TRACE_ID));

        assertThat(tree).singleElement().satisfies(root -> {
            assertThat(root.getAttributes()).extracting(SpanAttributeDTO::getAttributeKey)
                    .containsExactlyInAnyOrder("rule.node.id", "input.msg.data");
            assertThat(root.getChildren()).singleElement().satisfies(child ->
                    assertThat(child.getAttributes()).extracting(SpanAttributeDTO::getAttributeKey).containsExactly("rule.node.id"));
        });
        verify(traceDao).findSpansByTraceId(TENANT_ID.getId(), TRACE_ID);
        verifyNoMoreInteractions(traceDao);
    }

    private static SpanEntity span(String spanId, String parentSpanId, long startTime, String attributesJson) {
        return new SpanEntity(spanId, TRACE_ID, TENANT_ID.getId(), parentSpanId, "Rule Node", "INTERNAL", "svc",
                attributesJson, "[]", "[]", startTime, startTime + 5, 5L, "OK", null, startTime);
    }

}
