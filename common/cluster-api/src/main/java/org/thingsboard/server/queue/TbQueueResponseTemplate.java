// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue;

import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.Set;

public interface TbQueueResponseTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> {

    void subscribe();

    void subscribe(Set<TopicPartitionInfo> partitions);

    void launch(TbQueueHandler<Request, Response> handler);

    void stop();
}
