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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.discovery.TopicService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultTbQueueServiceTest {

    @Mock
    private QueueService queueServiceMock;
    @Mock
    private TbClusterService tbClusterServiceMock;
    @Mock
    private TbQueueAdmin tbQueueAdminMock;

    private TopicService topicService;
    private DefaultTbQueueService tbQueueService;

    private final TenantId tenantId = TenantId.SYS_TENANT_ID;

    @BeforeEach
    public void setUp() {
        topicService = new TopicService();
        tbQueueService = new DefaultTbQueueService(queueServiceMock, tbClusterServiceMock, tbQueueAdminMock, topicService);
    }

    private Queue newQueue(int partitions) {
        Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("testQueue");
        queue.setTopic("tb_rule_engine.testQueue");
        queue.setPartitions(partitions);
        return queue;
    }

    @Test
    public void givenQueuePrefix_whenSaveQueue_thenCreatesPrefixedTopics() {
        // queue.prefix = "thingsboard" (TB_QUEUE_PREFIX set)
        ReflectionTestUtils.setField(topicService, "prefix", "thingsboard");

        Queue queue = newQueue(2);
        when(queueServiceMock.saveQueue(queue)).thenReturn(queue);

        tbQueueService.saveQueue(queue);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(tbQueueAdminMock, times(2)).createTopicIfNotExists(topicCaptor.capture(), any(), anyBoolean());

        // All created topics must carry the prefix - this is the fix.
        assertThat(topicCaptor.getAllValues())
                .containsExactlyInAnyOrder(
                        "thingsboard.tb_rule_engine.testQueue.0",
                        "thingsboard.tb_rule_engine.testQueue.1");
        // No unprefixed (orphan-prone) topic must ever be created.
        assertThat(topicCaptor.getAllValues())
                .noneMatch(topic -> topic.equals("tb_rule_engine.testQueue.0")
                        || topic.equals("tb_rule_engine.testQueue.1"));
    }

    @Test
    public void givenNoQueuePrefix_whenSaveQueue_thenCreatesUnprefixedTopics() {
        // queue.prefix blank (TB_QUEUE_PREFIX not set) - default behavior preserved
        ReflectionTestUtils.setField(topicService, "prefix", "");

        Queue queue = newQueue(2);
        when(queueServiceMock.saveQueue(queue)).thenReturn(queue);

        tbQueueService.saveQueue(queue);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(tbQueueAdminMock, times(2)).createTopicIfNotExists(topicCaptor.capture(), any(), anyBoolean());

        assertThat(topicCaptor.getAllValues())
                .containsExactlyInAnyOrder(
                        "tb_rule_engine.testQueue.0",
                        "tb_rule_engine.testQueue.1");
    }

    @Test
    public void givenQueuePrefix_whenIncreasePartitions_thenOnlyNewPartitionsCreatedPrefixed() {
        ReflectionTestUtils.setField(topicService, "prefix", "thingsboard");

        Queue oldQueue = newQueue(2);
        oldQueue.setId(new QueueId(UUID.randomUUID()));
        Queue updatedQueue = newQueue(4);
        updatedQueue.setId(oldQueue.getId());

        when(queueServiceMock.findQueueById(tenantId, updatedQueue.getId())).thenReturn(oldQueue);
        when(queueServiceMock.saveQueue(updatedQueue)).thenReturn(updatedQueue);

        tbQueueService.saveQueue(updatedQueue);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(tbQueueAdminMock, times(2)).createTopicIfNotExists(topicCaptor.capture(), any(), anyBoolean());

        assertThat(topicCaptor.getAllValues())
                .containsExactlyInAnyOrder(
                        "thingsboard.tb_rule_engine.testQueue.2",
                        "thingsboard.tb_rule_engine.testQueue.3");
        verify(tbQueueAdminMock, never()).createTopicIfNotExists(eq("thingsboard.tb_rule_engine.testQueue.0"), any(), anyBoolean());
    }

}
