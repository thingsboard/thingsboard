/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.edqs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.edqs.EdqsConfig;
import org.thingsboard.server.queue.kafka.KafkaAdmin;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@ConditionalOnExpression("'${queue.edqs.sync.enabled:true}' == 'true' && '${queue.type:null}' == 'kafka'")
public class KafkaEdqsSyncService extends EdqsSyncService {

    private final boolean syncNeeded;

    public KafkaEdqsSyncService(KafkaAdmin kafkaAdmin, TopicService topicService, EdqsConfig edqsConfig) {
        this.syncNeeded = kafkaAdmin.areAllTopicsEmpty(IntStream.range(0, edqsConfig.getPartitions())
                .mapToObj(partition -> TopicPartitionInfo.builder()
                        .topic(topicService.buildTopicName(edqsConfig.getEventsTopic()))
                        .partition(partition)
                        .build().getFullTopicName())
                .collect(Collectors.toSet()));
    }

    @Override
    public boolean isSyncNeeded() {
        return syncNeeded;
    }

}
