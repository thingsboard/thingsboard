// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueLagStatsRow;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueShortInfo;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

class RuleEngineMonitoringServiceQueueLagTest {

    @Test
    @SuppressWarnings("unchecked")
    void givenLagLookupFailsForOneQueue_whenFlushed_thenOnlyKnownLagIsWritten() {
        QueueShortInfo main = new QueueShortInfo(UUID.randomUUID(), TenantId.SYS_TENANT_ID.getId(), "Main", 1, false);
        QueueShortInfo hp = new QueueShortInfo(UUID.randomUUID(), TenantId.SYS_TENANT_ID.getId(), "HighPriority", 1, false);

        KafkaAdmin kafkaAdmin = Mockito.mock(KafkaAdmin.class);
        // The HighPriority group failed, so it is missing from the result.
        Mockito.when(kafkaAdmin.findTotalLagForGroups(any())).thenReturn(Map.of("re-Main", 5L));
        ObjectProvider<KafkaAdmin> kafkaAdminProvider = Mockito.mock(ObjectProvider.class);
        Mockito.when(kafkaAdminProvider.getIfAvailable()).thenReturn(kafkaAdmin);
        TopicService topicService = Mockito.mock(TopicService.class);
        Mockito.when(topicService.buildConsumerGroupId(eq("re-"), any(), anyString(), isNull()))
                .thenAnswer(invocation -> "re-" + invocation.getArgument(2));
        PartitionService partitionService = Mockito.mock(PartitionService.class);
        Mockito.when(partitionService.isSystemTenantPartitionMine(ServiceType.TB_RULE_ENGINE)).thenReturn(true);
        QueueCacheService queueCacheService = Mockito.mock(QueueCacheService.class);
        Mockito.when(queueCacheService.getAllQueues()).thenReturn(List.of(main, hp));
        RuleEngineMonitoringDao dao = Mockito.mock(RuleEngineMonitoringDao.class);

        RuleEngineMonitoringService service = new RuleEngineMonitoringService(dao, Mockito.mock(TbTenantProfileCache.class),
                partitionService, topicService, kafkaAdminProvider,
                Mockito.mock(StatsFactory.class, Mockito.RETURNS_DEEP_STUBS), queueCacheService,
                30, 30, 1_000_000, 1_000_000, false);

        service.flushQueueLagStats();

        ArgumentCaptor<List<QueueLagStatsRow>> rows = ArgumentCaptor.forClass(List.class);
        Mockito.verify(dao).saveQueueLagStats(rows.capture());
        assertThat(rows.getValue()).singleElement().satisfies(row -> {
            assertThat(row.queueId()).isEqualTo(main.id());
            assertThat(row.lag()).isEqualTo(5L);
        });
    }

}
