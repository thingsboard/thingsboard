// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ttl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.sql.trace.TraceDao;
import org.thingsboard.server.queue.discovery.PartitionService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceCleanUpServiceTest {

    private static final long NOW = 1_700_000_000_000L;
    private static final long DAY_MS = 86_400_000L;

    @Mock
    private PartitionService partitionService;
    @Mock
    private TraceDao traceDao;
    @Mock
    private RuleEngineMonitoringDao ruleEngineMonitoringDao;
    @Mock
    private TopicPartitionInfo partitionInfo;

    private TraceCleanUpService service;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new TraceCleanUpService(partitionService, traceDao, ruleEngineMonitoringDao));
        ReflectionTestUtils.setField(service, "spansTtlInSec", 7 * 86_400L);
        ReflectionTestUtils.setField(service, "traceGroupsTtlInSec", 30 * 86_400L);
        ReflectionTestUtils.setField(service, "ruleEngineStatsTtlInSec", 14 * 86_400L);
        ReflectionTestUtils.setField(service, "removalBatchSize", 500);
        Mockito.doReturn(NOW).when(service).getCurrentTimeMillis();
        when(partitionService.resolve(any(), any(), any())).thenReturn(partitionInfo);
    }

    @Test
    void givenSystemPartitionIsMine_whenCleanUp_thenEachTableUsesItsOwnTtl() {
        when(partitionInfo.isMyPartition()).thenReturn(true);

        service.cleanUp();

        verify(traceDao).cleanUp(NOW - 7 * DAY_MS, NOW - 30 * DAY_MS, 500);
        verify(ruleEngineMonitoringDao).cleanUp(NOW - 14 * DAY_MS, 500);
        verify(traceDao, never()).cleanUpPartitionsCache(anyLong());
    }

    @Test
    void givenSystemPartitionIsNotMine_whenCleanUp_thenOnlyThePartitionCacheIsCleaned() {
        when(partitionInfo.isMyPartition()).thenReturn(false);

        service.cleanUp();

        verify(traceDao).cleanUpPartitionsCache(NOW - 7 * DAY_MS);
        verify(traceDao, never()).cleanUp(anyLong(), anyLong(), anyInt());
        verify(ruleEngineMonitoringDao, never()).cleanUp(anyLong(), anyInt());
    }

    @Test
    void givenZeroTtl_whenCleanUp_thenThatDataIsKept() {
        ReflectionTestUtils.setField(service, "traceGroupsTtlInSec", 0L);
        when(partitionInfo.isMyPartition()).thenReturn(true);

        service.cleanUp();

        verify(traceDao).cleanUp(NOW - 7 * DAY_MS, 0, 500);
    }

}
