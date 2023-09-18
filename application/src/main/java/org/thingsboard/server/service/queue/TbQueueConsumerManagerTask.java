package org.thingsboard.server.service.queue;

import lombok.Data;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.Set;

@Data
public class TbQueueConsumerManagerTask {

    private final ComponentLifecycleEvent event;
    private final Queue queue;
    private final Set<TopicPartitionInfo> partitions;

}
