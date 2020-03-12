package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.TbProtoQueueMsg;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class MsgPackCallback<T extends com.google.protobuf.GeneratedMessageV3> implements TbMsgCallback {
    private final CountDownLatch processingTimeoutLatch;
    private final ConcurrentMap<UUID, TbProtoQueueMsg<T>> ackMap;
    private final UUID id;

    public MsgPackCallback(UUID id, CountDownLatch processingTimeoutLatch, ConcurrentMap<UUID, TbProtoQueueMsg<T>> ackMap) {
        this.id = id;
        this.processingTimeoutLatch = processingTimeoutLatch;
        this.ackMap = ackMap;
    }

    @Override
    public void onSuccess() {
        if (ackMap.remove(id) != null && ackMap.isEmpty()) {
            processingTimeoutLatch.countDown();
        }
    }

    @Override
    public void onFailure(Throwable t) {
        TbProtoQueueMsg<T> message = ackMap.remove(id);
        log.warn("Failed to process message: {}", message.getValue(), t);
        if (ackMap.isEmpty()) {
            processingTimeoutLatch.countDown();
        }
    }
}
