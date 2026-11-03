// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.common.state;

import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;

import java.util.Collections;

public class DefaultQueueStateService<E extends TbQueueMsg, S extends TbQueueMsg> extends QueueStateService<E, S> {

    public DefaultQueueStateService(PartitionedQueueConsumerManager<E> eventConsumer) {
        super(eventConsumer, Collections.emptyList());
    }

}
