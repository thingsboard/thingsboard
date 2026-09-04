// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue;

import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.List;
import java.util.Set;

public interface TbQueueConsumer<T extends TbQueueMsg> {

    String getTopic();

    void subscribe();

    void subscribe(Set<TopicPartitionInfo> partitions);

    void stop();

    void unsubscribe();

    List<T> poll(long durationInMillis);

    void commit();

    boolean isStopped();

    List<String> getFullTopicNames();

}
