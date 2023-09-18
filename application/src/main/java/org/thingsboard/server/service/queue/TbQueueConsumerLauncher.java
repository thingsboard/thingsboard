package org.thingsboard.server.service.queue;

import lombok.Data;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Data
public class TbQueueConsumerLauncher {

    private final TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> consumer;
    private volatile Future<?> task;

    public void stop() {
        this.consumer.stop();
    }

    public void awaitStopped() throws ExecutionException, InterruptedException, TimeoutException {
        if (task != null) {
            this.task.get(3, TimeUnit.MINUTES);
        }
    }

    public void subscribe(Set<TopicPartitionInfo> partitions) {
        this.consumer.subscribe(partitions);
    }
}
