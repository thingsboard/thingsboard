/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.queue.pubsub;

import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.pubsub.v1.ProjectTopicName;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.io.IOException;

@Slf4j
public class TbPubSubAdmin implements TbQueueAdmin {

    private final TbPubSubSettings pubSubSettings;

    public TbPubSubAdmin(TbPubSubSettings pubSubSettings) {
        this.pubSubSettings = pubSubSettings;
    }

    @Override
    public void createTopicIfNotExists(String topic) {
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create()) {
            ProjectTopicName topicName = ProjectTopicName.of(pubSubSettings.getProjectId(), topic);
            topicAdminClient.createTopic(topicName);
        } catch (IOException e) {
            log.error("Failed to create topic: [{}]", topic, e);
            throw new RuntimeException("Failed to create topic.", e);
        }
    }
}
