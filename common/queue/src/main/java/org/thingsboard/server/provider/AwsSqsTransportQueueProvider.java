package org.thingsboard.server.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TbQueueAdmin;
import org.thingsboard.server.TbQueueConsumer;
import org.thingsboard.server.TbQueueCoreSettings;
import org.thingsboard.server.TbQueueProducer;
import org.thingsboard.server.TbQueueRequestTemplate;
import org.thingsboard.server.TbQueueRuleEngineSettings;
import org.thingsboard.server.TbQueueTransportApiSettings;
import org.thingsboard.server.TbQueueTransportNotificationSettings;
import org.thingsboard.server.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.common.TbProtoQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.kafka.TbNodeIdProvider;
import org.thingsboard.server.sqs.TbAwsSqsAdmin;
import org.thingsboard.server.sqs.TbAwsSqsConsumerTemplate;
import org.thingsboard.server.sqs.TbAwsSqsProducerTemplate;
import org.thingsboard.server.sqs.TbAwsSqsSettings;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs' && ('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-transport')")
@Slf4j
public class AwsSqsTransportQueueProvider implements TransportQueueProvider {
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbAwsSqsSettings sqsSettings;
    private final TbQueueAdmin admin;
    private final TbNodeIdProvider nodeIdProvider;

    public AwsSqsTransportQueueProvider(TbQueueCoreSettings coreSettings,
                                        TbQueueRuleEngineSettings ruleEngineSettings,
                                        TbQueueTransportApiSettings transportApiSettings,
                                        TbQueueTransportNotificationSettings transportNotificationSettings,
                                        TbAwsSqsSettings sqsSettings,
                                        TbNodeIdProvider nodeIdProvider) {
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.sqsSettings = sqsSettings;
        admin = new TbAwsSqsAdmin(sqsSettings);
        this.nodeIdProvider = nodeIdProvider;
    }

    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>, TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> getTransportApiRequestTemplate() {
        TbAwsSqsProducerTemplate<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>> producerTemplate =
                new TbAwsSqsProducerTemplate<>(admin, sqsSettings, transportApiSettings.getRequestsTopic());

        TbAwsSqsConsumerTemplate<TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> consumerTemplate =
                new TbAwsSqsConsumerTemplate<>(admin, sqsSettings,
                        transportApiSettings.getResponsesTopic() + "_" + nodeIdProvider.getNodeId(),
                        msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.TransportApiResponseMsg.parseFrom(msg.getData()), msg.getHeaders()));

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>, TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> templateBuilder = DefaultTbQueueRequestTemplate.builder();
        templateBuilder.queueAdmin(admin);
        templateBuilder.requestTemplate(producerTemplate);
        templateBuilder.responseTemplate(consumerTemplate);
        templateBuilder.maxPendingRequests(transportApiSettings.getMaxPendingRequests());
        templateBuilder.maxRequestTimeout(transportApiSettings.getMaxRequestsTimeout());
        templateBuilder.pollInterval(transportApiSettings.getResponsePollInterval());
        return templateBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getRuleEngineMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(admin, sqsSettings, transportApiSettings.getRequestsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> getTbCoreMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(admin, sqsSettings, transportApiSettings.getRequestsTopic());
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> getTransportNotificationsConsumer() {
        return new TbAwsSqsConsumerTemplate<>(admin, sqsSettings, transportNotificationSettings.getNotificationsTopic() + "_" + nodeIdProvider.getNodeId(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToTransportMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }
}
