/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.queue.pubsub;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.discovery.TopicPartitionInfo;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
public class TbPubSubConsumerTemplate<T extends TbQueueMsg> implements TbQueueConsumer<T> {

    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final String topic;
    private final TbQueueMsgDecoder<T> decoder;

    private volatile boolean subscribed;
    private volatile Set<TopicPartitionInfo> partitions;
    private ListeningExecutorService consumerExecutor;

    public TbPubSubConsumerTemplate(TbQueueAdmin admin, String topic, TbQueueMsgDecoder<T> decoder) {
        this.admin = admin;
        this.topic = topic;
        this.decoder = decoder;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public void subscribe() {
        partitions = Collections.singleton(new TopicPartitionInfo(topic, null, null));
        subscribed = false;
    }

    @Override
    public void subscribe(Set<TopicPartitionInfo> partitions) {
        this.partitions = partitions;
        subscribed = false;
    }

    @Override
    public void unsubscribe() {
        if (consumerExecutor != null) {
            consumerExecutor.shutdown();
        }
    }

    @Override
    public List<T> poll(long durationInMillis) {
        if (!subscribed && partitions == null) {
            try {
                Thread.sleep(durationInMillis);
            } catch (InterruptedException e) {
                log.debug("Failed to await subscription", e);
            }
        } else {
            if (!subscribed) {

            }
        }
        return Collections.emptyList();
    }

    @Override
    public void commit() {

    }

    public T decode() throws InvalidProtocolBufferException {

        return null;
    }

}
