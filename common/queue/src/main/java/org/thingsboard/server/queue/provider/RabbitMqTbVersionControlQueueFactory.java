/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.queue.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqAdmin;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqConsumerTemplate;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqProducerTemplate;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqQueueArguments;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqSettings;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueVersionControlSettings;

import javax.annotation.PreDestroy;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq' && '${service.type:null}'=='tb-vc-executor'")
public class RabbitMqTbVersionControlQueueFactory implements TbVersionControlQueueFactory {

    private final TbRabbitMqSettings rabbitMqSettings;
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueVersionControlSettings vcSettings;

    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin notificationAdmin;
    private final TbQueueAdmin vcAdmin;

    public RabbitMqTbVersionControlQueueFactory(TbRabbitMqSettings rabbitMqSettings,
                                                TbQueueCoreSettings coreSettings,
                                                TbQueueVersionControlSettings vcSettings,
                                                TbRabbitMqQueueArguments queueArguments
    ) {
        this.rabbitMqSettings = rabbitMqSettings;
        this.coreSettings = coreSettings;
        this.vcSettings = vcSettings;

        this.coreAdmin = new TbRabbitMqAdmin(this.rabbitMqSettings, queueArguments.getCoreArgs());
        this.notificationAdmin = new TbRabbitMqAdmin(this.rabbitMqSettings, queueArguments.getNotificationsArgs());
        this.vcAdmin = new TbRabbitMqAdmin(this.rabbitMqSettings, queueArguments.getVcArgs());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(coreAdmin, rabbitMqSettings, coreSettings.getUsageStatsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbRabbitMqProducerTemplate<>(notificationAdmin, rabbitMqSettings, coreSettings.getTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createToVersionControlMsgConsumer() {
        return new TbRabbitMqConsumerTemplate<>(vcAdmin, rabbitMqSettings, vcSettings.getTopic(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToVersionControlServiceMsg.parseFrom(msg.getData()), msg.getHeaders())
        );
    }

    @PreDestroy
    private void destroy() {
        if (coreAdmin != null) {
            coreAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
        if (vcAdmin != null) {
            vcAdmin.destroy();
        }
    }
}
