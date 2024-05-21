/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.queue.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.util.PropertyUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TbKafkaAdmin implements TbQueueAdmin {

    private final TbKafkaSettings settings;
    private final Map<String, String> topicConfigs;
    private final int numPartitions;
    private volatile Set<String> topics;

    private final short replicationFactor;

    public TbKafkaAdmin(TbKafkaSettings settings, Map<String, String> topicConfigs) {
        this.settings = settings;
        this.topicConfigs = topicConfigs;

        String numPartitionsStr = topicConfigs.get(TbKafkaTopicConfigs.NUM_PARTITIONS_SETTING);
        if (numPartitionsStr != null) {
            numPartitions = Integer.parseInt(numPartitionsStr);
            topicConfigs.remove("partitions");
        } else {
            numPartitions = 1;
        }
        replicationFactor = settings.getReplicationFactor();
    }

    @Override
    public void createTopicIfNotExists(String topic, String properties) {
        Set<String> topics = getTopics();
        if (topics.contains(topic)) {
            return;
        }
        try {
            NewTopic newTopic = new NewTopic(topic, numPartitions, replicationFactor).configs(PropertyUtils.getProps(topicConfigs, properties));
            createTopic(newTopic).values().get(topic).get();
            topics.add(topic);
        } catch (ExecutionException ee) {
            if (ee.getCause() instanceof TopicExistsException) {
                //do nothing
            } else {
                log.warn("[{}] Failed to create topic", topic, ee);
                throw new RuntimeException(ee);
            }
        } catch (Exception e) {
            log.warn("[{}] Failed to create topic", topic, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteTopic(String topic) {
        Set<String> topics = getTopics();
        if (topics.contains(topic)) {
            settings.getAdminClient().deleteTopics(Collections.singletonList(topic));
        } else {
            try {
                if (settings.getAdminClient().listTopics().names().get().contains(topic)) {
                    settings.getAdminClient().deleteTopics(Collections.singletonList(topic));
                } else {
                    log.warn("Kafka topic [{}] does not exist.", topic);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed to delete kafka topic [{}].", topic, e);
            }
        }
    }

    private Set<String> getTopics() {
        if (topics == null) {
            synchronized (this) {
                if (topics == null) {
                    topics = ConcurrentHashMap.newKeySet();
                    try {
                        topics.addAll(settings.getAdminClient().listTopics().names().get());
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Failed to get all topics.", e);
                    }
                }
            }
        }
        return topics;
    }

    public CreateTopicsResult createTopic(NewTopic topic) {
        return settings.getAdminClient().createTopics(Collections.singletonList(topic));
    }

    @Override
    public void destroy() {
    }

}
