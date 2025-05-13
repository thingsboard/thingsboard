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
package org.thingsboard.server.queue.task;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.gen.transport.TransportProtos.JobStatsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerStatsService;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.settings.TasksQueueConfig;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaTaskProcessorQueueFactory implements TaskProcessorQueueFactory {

    private final TopicService topicService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TasksQueueConfig tasksQueueConfig;
    private final TbKafkaSettings kafkaSettings;
    private final TbKafkaConsumerStatsService consumerStatsService;

    private final TbQueueAdmin tasksAdmin;

    public KafkaTaskProcessorQueueFactory(TopicService topicService,
                                          TbServiceInfoProvider serviceInfoProvider,
                                          TasksQueueConfig tasksQueueConfig,
                                          TbKafkaSettings kafkaSettings,
                                          TbKafkaConsumerStatsService consumerStatsService,
                                          TbKafkaTopicConfigs kafkaTopicConfigs) {
        this.topicService = topicService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.tasksQueueConfig = tasksQueueConfig;
        this.kafkaSettings = kafkaSettings;
        this.consumerStatsService = consumerStatsService;
        this.tasksAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTasksConfigs());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TaskProto>> createTaskConsumer(JobType jobType) {
        return TbKafkaConsumerTemplate.<TbProtoQueueMsg<TaskProto>>builder()
                .settings(kafkaSettings)
                .topic(topicService.buildTopicName(jobType.getTasksTopic()))
                .clientId(jobType.name().toLowerCase() + "-task-consumer-" + serviceInfoProvider.getServiceId())
                .groupId(topicService.buildTopicName(jobType.name().toLowerCase() + "-task-consumer-group"))
                .decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), TaskProto.parseFrom(msg.getData()), msg.getHeaders()))
                .admin(tasksAdmin)
                .statsService(consumerStatsService)
                .build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<JobStatsMsg>> createJobStatsProducer() {
        return TbKafkaProducerTemplate.<TbProtoQueueMsg<JobStatsMsg>>builder()
                .clientId("job-stats-producer-" + serviceInfoProvider.getServiceId())
                .defaultTopic(topicService.buildTopicName(tasksQueueConfig.getStatsTopic()))
                .settings(kafkaSettings)
                .admin(tasksAdmin)
                .build();
    }

}
