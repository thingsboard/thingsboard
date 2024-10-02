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
package org.thingsboard.server.queue.provider;

import com.google.protobuf.util.JsonFormat;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsRequest;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsResponse;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueEdgeSettings;
import org.thingsboard.server.queue.settings.TbQueueRemoteJsInvokeSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;
import org.thingsboard.server.queue.settings.TbQueueVersionControlSettings;
import org.thingsboard.server.queue.sqs.TbAwsSqsAdmin;
import org.thingsboard.server.queue.sqs.TbAwsSqsConsumerTemplate;
import org.thingsboard.server.queue.sqs.TbAwsSqsProducerTemplate;
import org.thingsboard.server.queue.sqs.TbAwsSqsQueueAttributes;
import org.thingsboard.server.queue.sqs.TbAwsSqsSettings;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs' && '${service.type:null}'=='monolith'")
public class AwsSqsMonolithQueueFactory implements TbCoreQueueFactory, TbRuleEngineQueueFactory, TbVersionControlQueueFactory {

    private final TopicService topicService;
    private final TbQueueCoreSettings coreSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbAwsSqsSettings sqsSettings;
    private final TbQueueVersionControlSettings vcSettings;
    private final TbQueueEdgeSettings edgeSettings;
    private final TbQueueRemoteJsInvokeSettings jsInvokeSettings;

    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin ruleEngineAdmin;
    private final TbQueueAdmin jsExecutorAdmin;
    private final TbQueueAdmin transportApiAdmin;
    private final TbQueueAdmin notificationAdmin;
    private final TbQueueAdmin otaAdmin;
    private final TbQueueAdmin vcAdmin;
    private final TbQueueAdmin edgeAdmin;

    public AwsSqsMonolithQueueFactory(TopicService topicService, TbQueueCoreSettings coreSettings,
                                      TbQueueRuleEngineSettings ruleEngineSettings,
                                      TbServiceInfoProvider serviceInfoProvider,
                                      TbQueueTransportApiSettings transportApiSettings,
                                      TbQueueTransportNotificationSettings transportNotificationSettings,
                                      TbAwsSqsSettings sqsSettings,
                                      TbQueueVersionControlSettings vcSettings,
                                      TbQueueEdgeSettings edgeSettings,
                                      TbAwsSqsQueueAttributes sqsQueueAttributes,
                                      TbQueueRemoteJsInvokeSettings jsInvokeSettings) {
        this.topicService = topicService;
        this.coreSettings = coreSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.sqsSettings = sqsSettings;
        this.vcSettings = vcSettings;
        this.edgeSettings = edgeSettings;
        this.jsInvokeSettings = jsInvokeSettings;

        this.coreAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getCoreAttributes());
        this.ruleEngineAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getRuleEngineAttributes());
        this.jsExecutorAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getJsExecutorAttributes());
        this.transportApiAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getTransportApiAttributes());
        this.notificationAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getNotificationsAttributes());
        this.otaAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getOtaAttributes());
        this.vcAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getVcAttributes());
        this.edgeAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getEdgeAttributes());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings, topicService.buildTopicName(transportNotificationSettings.getNotificationsTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(ruleEngineAdmin, sqsSettings, topicService.buildTopicName(ruleEngineSettings.getTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings, topicService.buildTopicName(ruleEngineSettings.getTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings,
                topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToVersionControlServiceMsg>> createToVersionControlMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(vcAdmin, sqsSettings, topicService.buildTopicName(vcSettings.getTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToVersionControlServiceMsg.parseFrom(msg.getData()), msg.getHeaders())
        );
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> createToRuleEngineMsgConsumer(Queue configuration) {
        return new TbAwsSqsConsumerTemplate<>(ruleEngineAdmin, sqsSettings, topicService.buildTopicName(configuration.getTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createToRuleEngineNotificationsMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(notificationAdmin, sqsSettings,
                topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> createToCoreMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreNotificationMsg>> createToCoreNotificationsMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(notificationAdmin, sqsSettings,
                topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> createTransportApiRequestConsumer() {
        return new TbAwsSqsConsumerTemplate<>(transportApiAdmin, sqsSettings, topicService.buildTopicName(transportApiSettings.getRequestsTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiResponseProducer() {
        return new TbAwsSqsProducerTemplate<>(transportApiAdmin, sqsSettings, topicService.buildTopicName(transportApiSettings.getResponsesTopic()));
    }

    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<RemoteJsRequest>, TbProtoQueueMsg<RemoteJsResponse>> createRemoteJsRequestTemplate() {
        TbQueueProducer<TbProtoJsQueueMsg<RemoteJsRequest>> producer = new TbAwsSqsProducerTemplate<>(jsExecutorAdmin, sqsSettings, jsInvokeSettings.getRequestTopic());
        TbQueueConsumer<TbProtoQueueMsg<RemoteJsResponse>> consumer = new TbAwsSqsConsumerTemplate<>(jsExecutorAdmin, sqsSettings,
                jsInvokeSettings.getResponseTopic() + "_" + serviceInfoProvider.getServiceId(),
                msg -> {
                    RemoteJsResponse.Builder builder = RemoteJsResponse.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
                    return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
                });

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoJsQueueMsg<RemoteJsRequest>, TbProtoQueueMsg<RemoteJsResponse>> builder = DefaultTbQueueRequestTemplate.builder();
        builder.queueAdmin(jsExecutorAdmin);
        builder.requestTemplate(producer);
        builder.responseTemplate(consumer);
        builder.maxPendingRequests(jsInvokeSettings.getMaxPendingRequests());
        builder.maxRequestTimeout(jsInvokeSettings.getMaxRequestsTimeout());
        builder.pollInterval(jsInvokeSettings.getResponsePollInterval());
        return builder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getUsageStatsTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToUsageStatsServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(otaAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getOtaPackageTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToOtaPackageStateServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(otaAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getOtaPackageTopic()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToVersionControlServiceMsg>> createVersionControlMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(vcAdmin, sqsSettings, topicService.buildTopicName(vcSettings.getTopic()));
    }

    public TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> createHousekeeperMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getHousekeeperTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> createHousekeeperMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getHousekeeperTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToHousekeeperServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> createHousekeeperReprocessingMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getHousekeeperReprocessingTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> createHousekeeperReprocessingMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getHousekeeperReprocessingTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToHousekeeperServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToEdgeMsg>> createEdgeMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(edgeAdmin, sqsSettings, topicService.buildTopicName(edgeSettings.getTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToEdgeMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdgeMsg>> createEdgeMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(edgeAdmin, sqsSettings, topicService.buildTopicName(edgeSettings.getTopic()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToEdgeNotificationMsg>> createToEdgeNotificationsMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(notificationAdmin, sqsSettings,
                topicService.getEdgeNotificationsTopic(serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToEdgeNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdgeNotificationMsg>> createEdgeNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings,
                topicService.getEdgeNotificationsTopic(serviceInfoProvider.getServiceId()).getFullTopicName());
    }

    @PreDestroy
    private void destroy() {
        if (coreAdmin != null) {
            coreAdmin.destroy();
        }
        if (ruleEngineAdmin != null) {
            ruleEngineAdmin.destroy();
        }
        if (jsExecutorAdmin != null) {
            jsExecutorAdmin.destroy();
        }
        if (transportApiAdmin != null) {
            transportApiAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
        if (otaAdmin != null) {
            otaAdmin.destroy();
        }
        if (vcAdmin != null) {
            vcAdmin.destroy();
        }
        if (edgeAdmin != null) {
            edgeAdmin.destroy();
        }
    }
}
