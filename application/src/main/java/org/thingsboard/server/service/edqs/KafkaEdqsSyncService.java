// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
