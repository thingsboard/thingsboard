package org.thingsboard.server.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TbQueueAdmin;
import org.thingsboard.server.TbQueueConsumer;
import org.thingsboard.server.TbQueueCoreSettings;
import org.thingsboard.server.TbQueueProducer;
import org.thingsboard.server.TbQueueRuleEngineSettings;
import org.thingsboard.server.TbQueueTransportApiSettings;
import org.thingsboard.server.TbQueueTransportNotificationSettings;
import org.thingsboard.server.common.TbProtoQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.sqs.TbAwsSqsAdmin;
import org.thingsboard.server.sqs.TbAwsSqsConsumerTemplate;
import org.thingsboard.server.sqs.TbAwsSqsProducerTemplate;
import org.thingsboard.server.sqs.TbAwsSqsSettings;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs' && '${service.type:null}'=='monolith'")
public class AwsSqsMonolithQueueProvider implements TbCoreQueueProvider, TbRuleEngineQueueProvider {
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbAwsSqsSettings sqsSettings;
    private final TbQueueAdmin admin;
    private TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> tbCoreProducer;

    public AwsSqsMonolithQueueProvider(TbQueueCoreSettings coreSettings,
                                       TbQueueRuleEngineSettings ruleEngineSettings,
                                       TbQueueTransportApiSettings transportApiSettings,
                                       TbQueueTransportNotificationSettings transportNotificationSettings,
                                       TbAwsSqsSettings sqsSettings) {
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.sqsSettings = sqsSettings;
        admin = new TbAwsSqsAdmin(sqsSettings);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> getTransportNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(admin, sqsSettings, transportNotificationSettings.getNotificationsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getRuleEngineMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(admin, sqsSettings, ruleEngineSettings.getTopic());
    }

    //TODO 2.5 Singleton
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> getTbCoreMsgProducer() {
        if (tbCoreProducer == null) {
            synchronized (this) {
                if (tbCoreProducer == null) {
                    tbCoreProducer = new TbAwsSqsProducerTemplate<>(admin, sqsSettings, coreSettings.getTopic());
                }
            }
        }
        return tbCoreProducer;
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getToRuleEngineMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(admin, sqsSettings, ruleEngineSettings.getTopic(), msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToRuleEngineMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> getToCoreMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(admin, sqsSettings, coreSettings.getTopic(), msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>> getTransportApiRequestConsumer() {
        return new TbAwsSqsConsumerTemplate<>(admin, sqsSettings, transportApiSettings.getRequestsTopic(), msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> getTransportApiResponseProducer() {
        return new TbAwsSqsProducerTemplate<>(admin, sqsSettings, transportApiSettings.getResponsesTopic());
    }
}
