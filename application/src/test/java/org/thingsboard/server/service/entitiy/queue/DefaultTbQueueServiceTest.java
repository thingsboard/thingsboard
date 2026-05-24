/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.entitiy.queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.discovery.TopicService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultTbQueueServiceTest {

    @Mock
    private QueueService queueService;
    @Mock
    private TbClusterService tbClusterService;
    @Mock
    private TbQueueAdmin tbQueueAdmin;
    @Mock
    private TopicService topicService;

    private DefaultTbQueueService service;

    @BeforeEach
    void setUp() {
        service = new DefaultTbQueueService(queueService, tbClusterService, tbQueueAdmin, topicService);
    }

    @Test
    void saveQueueCreatesPrefixedKafkaTopicsWhenQueuePrefixIsConfigured() {
        Queue queue = queue("tb_rule_engine.testQueue", 2);
        when(queueService.saveQueue(queue)).thenReturn(queue);
        when(topicService.buildTopicName("tb_rule_engine.testQueue")).thenReturn("thingsboard.tb_rule_engine.testQueue");

        service.saveQueue(queue);

        verify(tbQueueAdmin).createTopicIfNotExists(eq("thingsboard.tb_rule_engine.testQueue.0"), isNull(), eq(true));
        verify(tbQueueAdmin).createTopicIfNotExists(eq("thingsboard.tb_rule_engine.testQueue.1"), isNull(), eq(true));
        verify(tbQueueAdmin, never()).createTopicIfNotExists(eq("tb_rule_engine.testQueue.0"), isNull(), eq(true));
        verify(tbClusterService).onQueuesUpdate(List.of(queue));
    }

    @Test
    void saveQueueCreatesOnlyNewPrefixedPartitionsWhenPartitionCountIncreases() {
        QueueId queueId = new QueueId(UUID.randomUUID());
        Queue oldQueue = queue("tb_rule_engine.testQueue", 1);
        oldQueue.setId(queueId);
        Queue queue = queue("tb_rule_engine.testQueue", 3);
        queue.setId(queueId);

        when(queueService.findQueueById(queue.getTenantId(), queueId)).thenReturn(oldQueue);
        when(queueService.saveQueue(queue)).thenReturn(queue);
        when(topicService.buildTopicName("tb_rule_engine.testQueue")).thenReturn("thingsboard.tb_rule_engine.testQueue");

        service.saveQueue(queue);

        verify(tbQueueAdmin, never()).createTopicIfNotExists(eq("thingsboard.tb_rule_engine.testQueue.0"), isNull(), eq(true));
        verify(tbQueueAdmin).createTopicIfNotExists(eq("thingsboard.tb_rule_engine.testQueue.1"), isNull(), eq(true));
        verify(tbQueueAdmin).createTopicIfNotExists(eq("thingsboard.tb_rule_engine.testQueue.2"), isNull(), eq(true));
        verify(tbClusterService).onQueuesUpdate(List.of(queue));
    }

    @Test
    void saveQueueDoesNotCreateKafkaTopicsWhenPartitionCountIsUnchanged() {
        QueueId queueId = new QueueId(UUID.randomUUID());
        Queue oldQueue = queue("tb_rule_engine.testQueue", 2);
        oldQueue.setId(queueId);
        Queue queue = queue("tb_rule_engine.testQueue", 2);
        queue.setId(queueId);

        when(queueService.findQueueById(queue.getTenantId(), queueId)).thenReturn(oldQueue);
        when(queueService.saveQueue(queue)).thenReturn(queue);
        when(topicService.buildTopicName("tb_rule_engine.testQueue")).thenReturn("thingsboard.tb_rule_engine.testQueue");

        service.saveQueue(queue);

        verifyNoInteractions(tbQueueAdmin);
        verify(tbClusterService).onQueuesUpdate(List.of(queue));
    }

    @Test
    void saveQueueDoesNotPersistQueuePrefixIntoQueueTopic() {
        Queue queue = queue("tb_rule_engine.testQueue", 1);
        when(queueService.saveQueue(queue)).thenReturn(queue);
        when(topicService.buildTopicName("tb_rule_engine.testQueue")).thenReturn("thingsboard.tb_rule_engine.testQueue");

        service.saveQueue(queue);

        assertEquals("tb_rule_engine.testQueue", queue.getTopic());
        verify(tbQueueAdmin).createTopicIfNotExists(eq("thingsboard.tb_rule_engine.testQueue.0"), isNull(), eq(true));
        verify(tbClusterService).onQueuesUpdate(List.of(queue));
    }

    private Queue queue(String topic, int partitions) {
        Queue queue = new Queue();
        queue.setName("testQueue");
        queue.setTopic(topic);
        queue.setTenantId(TenantId.SYS_TENANT_ID);
        queue.setPartitions(partitions);
        return queue;
    }

}
