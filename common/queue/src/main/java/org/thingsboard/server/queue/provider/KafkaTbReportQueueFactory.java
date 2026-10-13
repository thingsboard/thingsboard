// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.queue.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToTbReportNotificationMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerStatsService;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.util.TbReportComponent;

@Component
@TbReportComponent
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
@Slf4j
public class KafkaTbReportQueueFactory implements TbReportQueueFactory {

    private final TbKafkaSettings kafkaSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbKafkaConsumerStatsService consumerStatsService;
    private final TopicService topicService;

    private final TbQueueAdmin notificationAdmin;

    public KafkaTbReportQueueFactory(TbKafkaSettings kafkaSettings,
                                     TbServiceInfoProvider serviceInfoProvider,
                                     TbKafkaConsumerStatsService consumerStatsService,
                                     TbKafkaTopicConfigs kafkaTopicConfigs,
                                     TopicService topicService) {
        this.kafkaSettings = kafkaSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.consumerStatsService = consumerStatsService;
        this.topicService = topicService;
        this.notificationAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getNotificationsConfigs());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToTbReportNotificationMsg>> createTbReportNotificationsConsumer() {
        return TbKafkaConsumerTemplate.<TbProtoQueueMsg<TransportProtos.ToTbReportNotificationMsg>>builder()
                .settings(kafkaSettings)
                .topic(topicService.getNotificationsTopic(ServiceType.TB_REPORT, serviceInfoProvider.getServiceId()).getFullTopicName())
                .clientId("tb-report-notifications-consumer-" + serviceInfoProvider.getServiceId())
                .groupId(topicService.buildTopicName("tb-report-notifications-consumer-group-" + serviceInfoProvider.getServiceId()))
                .decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToTbReportNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()))
                .admin(notificationAdmin)
                .statsService(consumerStatsService)
                .build();
    }

}
