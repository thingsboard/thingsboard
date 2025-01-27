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
package org.thingsboard.server.service.edqs;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.edqs.EdqsQueue;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.edqs.sync_enabled:true}' == 'true' && '${queue.type:null}' == 'kafka'")
public class KafkaEdqsSyncService extends EdqsSyncService {

    private final TbKafkaSettings kafkaSettings;
    private TbKafkaAdmin kafkaAdmin;

    @PostConstruct
    private void init() {
        kafkaAdmin = new TbKafkaAdmin(kafkaSettings, Collections.emptyMap());
    }

    @Override
    public boolean isSyncNeeded() {
        return kafkaAdmin.isTopicEmpty(EdqsQueue.STATE.getTopic());
    }


}
