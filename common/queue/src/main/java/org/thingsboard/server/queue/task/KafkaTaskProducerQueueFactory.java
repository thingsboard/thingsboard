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
package org.thingsboard.server.queue.task;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;

@Component
@ConditionalOnExpression("'${queue.type:null}' == 'kafka' && ('${service.type:null}' == 'monolith' || " +
                         "'${service.type:null}' == 'tb-core' || '${service.type:null}' == 'tb-rule-engine')")
public class KafkaTaskProducerQueueFactory implements TaskProducerQueueFactory {

    private final TopicService topicService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbKafkaSettings kafkaSettings;
    private final TbQueueAdmin tasksAdmin;

    KafkaTaskProducerQueueFactory(TopicService topicService,
                                  TbServiceInfoProvider serviceInfoProvider,
                                  TbKafkaSettings kafkaSettings,
                                  TbKafkaTopicConfigs kafkaTopicConfigs) {
        this.topicService = topicService;
        this.kafkaSettings = kafkaSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.tasksAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTasksConfigs());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TaskProto>> createTaskProducer(JobType jobType) {
        return TbKafkaProducerTemplate.<TbProtoQueueMsg<TaskProto>>builder()
                .clientId(jobType.name().toLowerCase() + "-task-producer-" + serviceInfoProvider.getServiceId())
                .defaultTopic(topicService.buildTopicName(jobType.getTasksTopic()))
                .settings(kafkaSettings)
                .admin(tasksAdmin)
                .build();
    }

}
