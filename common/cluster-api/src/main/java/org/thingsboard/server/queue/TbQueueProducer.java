// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue;

import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

public interface TbQueueProducer<T extends TbQueueMsg> {

    String getDefaultTopic();

    void send(TopicPartitionInfo tpi, T msg, TbQueueCallback callback);

    void stop();

}
