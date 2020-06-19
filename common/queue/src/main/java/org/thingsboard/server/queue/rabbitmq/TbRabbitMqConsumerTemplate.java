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
package org.thingsboard.server.queue.rabbitmq;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.GetResponse;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.common.AbstractTbQueueConsumerTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
public class TbRabbitMqConsumerTemplate<T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<GetResponse, T> {

    private final Gson gson = new Gson();
    private final TbQueueAdmin admin;
    private final TbQueueMsgDecoder<T> decoder;
    private final Channel channel;
    private final Connection connection;

    private volatile Set<String> queues;

    public TbRabbitMqConsumerTemplate(TbQueueAdmin admin, TbRabbitMqSettings rabbitMqSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        super(topic);
        this.admin = admin;
        this.decoder = decoder;
        try {
            connection = rabbitMqSettings.getConnectionFactory().newConnection();
        } catch (IOException | TimeoutException e) {
            log.error("Failed to create connection.", e);
            throw new RuntimeException("Failed to create connection.", e);
        }
        try {
            channel = connection.createChannel();
        } catch (IOException e) {
            log.error("Failed to create chanel.", e);
            throw new RuntimeException("Failed to create chanel.", e);
        }
        stopped = false;
    }

    @Override
    protected List<GetResponse> doPoll(long durationInMillis) {
        List<GetResponse> result = queues.stream()
                .map(queue -> {
                    try {
                        return channel.basicGet(queue, false);
                    } catch (IOException e) {
                        log.error("Failed to get messages from queue: [{}]", queue);
                        throw new RuntimeException("Failed to get messages from queue.", e);
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
        if (result.size() > 0) {
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected void doSubscribe(List<String> topicNames) {
        queues = partitions.stream()
                .map(TopicPartitionInfo::getFullTopicName)
                .collect(Collectors.toSet());
        queues.forEach(admin::createTopicIfNotExists);
    }

    @Override
    protected void doCommit() {
        try {
            channel.basicAck(0, true);
        } catch (IOException e) {
            log.error("Failed to ack messages.", e);
        }
    }

    @Override
    protected void doUnsubscribe() {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                log.error("Failed to close the channel.");
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                log.error("Failed to close the connection.");
            }
        }
    }

    public T decode(GetResponse message) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(new String(message.getBody()), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }
}
