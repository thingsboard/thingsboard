// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Data
public class TbTopicWithConsumerPerPartition {
    private final String topic;
    @Getter
    private final ReentrantLock lock = new ReentrantLock(); //NonfairSync
    private volatile Set<TopicPartitionInfo> partitions = Collections.emptySet();
    private final ConcurrentMap<TopicPartitionInfo, TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>>> consumers = new ConcurrentHashMap<>();
    private final Queue<Set<TopicPartitionInfo>> subscribeQueue = new ConcurrentLinkedQueue<>();
}
