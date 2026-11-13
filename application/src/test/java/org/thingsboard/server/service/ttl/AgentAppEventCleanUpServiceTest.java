// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ttl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAppEventCleanUpServiceTest {

    private static final long DEFAULT_TTL_SEC = 604800L;
    private static final int REMOVAL_BATCH_SIZE = 5000;

    @Mock
    private PartitionService partitionService;
    @Mock
    private AgentAppEventService agentAppEventService;

    private AgentAppEventCleanUpService service;

    @BeforeEach
    void setUp() {
        service = new AgentAppEventCleanUpService(partitionService, agentAppEventService);
        ReflectionTestUtils.setField(service, "ttlInSec", DEFAULT_TTL_SEC);
        ReflectionTestUtils.setField(service, "removalBatchSize", REMOVAL_BATCH_SIZE);
    }

    @Test
    void cleanUp_skipsWhenNotMyPartition() {
        when(partitionService.isSystemTenantPartitionMine(any())).thenReturn(false);

        service.cleanUp();

        verify(agentAppEventService, never()).cleanUpExpiredEvents(any(long.class), any(int.class));
    }

    @Test
    void cleanUp_delegatesWithCorrectExpirationTs() {
        when(partitionService.isSystemTenantPartitionMine(any())).thenReturn(true);

        long before = System.currentTimeMillis();
        service.cleanUp();
        long after = System.currentTimeMillis();

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(agentAppEventService).cleanUpExpiredEvents(captor.capture(), eq(REMOVAL_BATCH_SIZE));

        long expectedMin = before - TimeUnit.SECONDS.toMillis(DEFAULT_TTL_SEC);
        long expectedMax = after - TimeUnit.SECONDS.toMillis(DEFAULT_TTL_SEC);
        assertThat(captor.getValue()).isBetween(expectedMin, expectedMax);
    }

    @Test
    void cleanUp_usesConfiguredTtl() {
        long customTtlSec = 86400L;
        ReflectionTestUtils.setField(service, "ttlInSec", customTtlSec);
        when(partitionService.isSystemTenantPartitionMine(any())).thenReturn(true);

        long before = System.currentTimeMillis();
        service.cleanUp();
        long after = System.currentTimeMillis();

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(agentAppEventService).cleanUpExpiredEvents(captor.capture(), eq(REMOVAL_BATCH_SIZE));

        long expectedMin = before - TimeUnit.SECONDS.toMillis(customTtlSec);
        long expectedMax = after - TimeUnit.SECONDS.toMillis(customTtlSec);
        assertThat(captor.getValue()).isBetween(expectedMin, expectedMax);
    }
}
