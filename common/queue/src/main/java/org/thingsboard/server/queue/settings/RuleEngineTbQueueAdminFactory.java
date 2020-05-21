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
package org.thingsboard.server.queue.settings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.pubsub.TbPubSubAdmin;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqAdmin;
import org.thingsboard.server.queue.sqs.TbAwsSqsAdmin;

@Configuration
public class RuleEngineTbQueueAdminFactory {

    @Autowired(required = false)
    private TbKafkaTopicConfigs kafkaTopicConfigs;
    @Autowired(required = false)
    private TbKafkaSettings kafkaSettings;

    @Autowired(required = false)
    private TbAwsSqsQueueAttributes awsSqsQueueAttributes;
    @Autowired(required = false)
    private TbAwsSqsSettings awsSqsSettings;

    @Autowired(required = false)
    private TbPubSubSubscriptionSettings pubSubSubscriptionSettings;
    @Autowired(required = false)
    private TbPubSubSettings pubSubSettings;

    @Autowired(required = false)
    private TbRabbitMqQueueArguments rabbitMqQueueArguments;
    @Autowired(required = false)
    private TbRabbitMqSettings rabbitMqSettings;

    @Autowired(required = false)
    private TbServiceBusQueueConfigs serviceBusQueueConfigs;
    @Autowired(required = false)
    private TbServiceBusSettings serviceBusSettings;

    @ConditionalOnExpression("'${queue.type:null}'=='kafka'")
    @Bean
    public TbQueueAdmin createKafkaAdmin() {
        return new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getRuleEngineConfigs());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='aws-sqs'")
    @Bean
    public TbQueueAdmin createAwsSqsAdmin() {
        return new TbAwsSqsAdmin(awsSqsSettings, awsSqsQueueAttributes.getRuleEngineAttributes());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='pubsub'")
    @Bean
    public TbQueueAdmin createPubSubAdmin() {
        return new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getRuleEngineSettings());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='rabbitmq'")
    @Bean
    public TbQueueAdmin createRabbitMqAdmin() {
        return new TbRabbitMqAdmin(rabbitMqSettings, rabbitMqQueueArguments.getRuleEngineArgs());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='service-bus'")
    @Bean
    public TbQueueAdmin createServiceBusAdmin() {
        return new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getRuleEngineConfigs());
    }

    @ConditionalOnExpression("'${queue.type:null}'=='in-memory'")
    @Bean
    public TbQueueAdmin createInMemoryAdmin() {
        return new TbQueueAdmin() {

            @Override
            public void createTopicIfNotExists(String topic) {

            }

            @Override
            public void deleteTopic(String topic) {

            }

            @Override
            public void destroy() {

            }
        };
    }
}
