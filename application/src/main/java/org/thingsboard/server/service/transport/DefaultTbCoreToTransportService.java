package org.thingsboard.server.service.transport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.TbQueueCallback;
import org.thingsboard.server.TbQueueMsgMetadata;
import org.thingsboard.server.TbQueueProducer;
import org.thingsboard.server.common.TbProtoQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceActorToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.provider.TbCoreQueueProvider;

import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core')")
public class DefaultTbCoreToTransportService implements TbCoreToTransportService {

    private final TbCoreQueueProvider tbCoreQueueProvider;
    private final TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> tbTransportProducer;

    @Value("${queue.notifications.topic}")
    private String notificationsTopic;

    public DefaultTbCoreToTransportService(TbCoreQueueProvider tbCoreQueueProvider) {
        this.tbCoreQueueProvider = tbCoreQueueProvider;
        this.tbTransportProducer = tbCoreQueueProvider.getTransportMsgProducer();
    }

    @Override
    public void process(String nodeId, DeviceActorToTransportMsg msg) {
        process(nodeId, msg, null, null);
    }

    @Override
    public void process(String nodeId, DeviceActorToTransportMsg msg, Runnable onSuccess, Consumer<Throwable> onFailure) {
        String topic = notificationsTopic + "." + nodeId;
        UUID sessionId = new UUID(msg.getSessionIdMSB(), msg.getSessionIdLSB());
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setToDeviceSessionMsg(msg).build();
        log.trace("[{}][{}] Pushing session data to topic: {}", topic, sessionId, transportMsg);
        TbProtoQueueMsg<ToTransportMsg> queueMsg = new TbProtoQueueMsg<>(sessionId, transportMsg);
        tbTransportProducer.send(topic, queueMsg, new QueueCallbackAdaptor(onSuccess, onFailure));
    }

    private static class QueueCallbackAdaptor implements TbQueueCallback {
        private final Runnable onSuccess;
        private final Consumer<Throwable> onFailure;

        QueueCallbackAdaptor(Runnable onSuccess, Consumer<Throwable> onFailure) {
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }

        @Override
        public void onFailure(Throwable t) {
            if (onFailure != null) {
                onFailure.accept(t);
            }
        }
    }
}
