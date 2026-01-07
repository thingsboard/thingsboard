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
package org.thingsboard.server.queue.kafka;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbEdgeQueueAdmin;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.util.PropertyUtils;

import java.util.Map;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TbKafkaAdmin implements TbQueueAdmin, TbEdgeQueueAdmin {

    private final TbKafkaSettings settings;
    private final Map<String, String> topicConfigs;
    @Getter
    private final int numPartitions;

    public TbKafkaAdmin(TbKafkaSettings settings, Map<String, String> topicConfigs) {
        this.settings = settings;
        this.topicConfigs = topicConfigs;
        String numPartitionsStr = topicConfigs.get(TbKafkaTopicConfigs.NUM_PARTITIONS_SETTING);
        if (numPartitionsStr != null) {
            numPartitions = Integer.parseInt(numPartitionsStr);
        } else {
            numPartitions = 1;
        }
    }

    @Override
    public void createTopicIfNotExists(String topic, String properties, boolean force) {
        settings.getAdmin().createTopicIfNotExists(topic, PropertyUtils.getProps(topicConfigs, properties), force);
    }

    @Override
    public void deleteTopic(String topic) {
        settings.getAdmin().deleteTopic(topic);
    }

    @Override
    public void destroy() {
    }

    /**
     * Sync edge notifications offsets from a fat group to a single group per edge
     * */
    public void syncEdgeNotificationsOffsets(String fatGroupId, String newGroupId) {
        try {
            log.info("syncEdgeNotificationsOffsets [{}][{}]", fatGroupId, newGroupId);
            settings.getAdmin().syncOffsetsUnsafe(fatGroupId, newGroupId, newGroupId);
        } catch (Exception e) {
            log.warn("Failed to syncEdgeNotificationsOffsets from {} to {}", fatGroupId, newGroupId, e);
        }
    }

}
