/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.service.queue.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.service.queue.TbAbstractMsgQueueService;
import org.thingsboard.server.service.queue.TbMsgQueuePack;
import org.thingsboard.server.service.queue.TbMsgQueueState;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "backpressure", value = "type", havingValue = "rabbit")
public class TbRabbitMqMsgQueueService extends TbAbstractMsgQueueService {

    @Value("${backpressure.rabbitmq.exchange_name}")
    private String exchangeName;
    @Value("${backpressure.rabbitmq.routing_key}")
    private String routingKey;
    @Value("${backpressure.rabbitmq.host}")
    private String host;
    @Value("${backpressure.rabbitmq.port}")
    private int port;
    @Value("${backpressure.rabbitmq.vhost}")
    private String virtualHost;
    @Value("${backpressure.rabbitmq.user}")
    private String username;
    @Value("${backpressure.rabbitmq.pass}")
    private String password;
    @Value("${backpressure.rabbitmq.automatic_recovery_enabled}")
    private boolean automaticRecoveryEnabled;
    @Value("${backpressure.rabbitmq.connection_timeout}")
    private int connectionTimeout;
    @Value("${backpressure.rabbitmq.handshake_timeout}")
    private int handshakeTimeout;

    @Value("${backpressure.rabbitmq.queue_name}")
    private String queueName;

    private Connection connection;
    private Channel channel;

    @PostConstruct
    private void init() {
        ackMap.put(collectiveTenantId, new AtomicBoolean(true));
        specialTenants.forEach(tenantId -> ackMap.put(tenantId, new AtomicBoolean(true)));

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setVirtualHost(virtualHost);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setAutomaticRecoveryEnabled(automaticRecoveryEnabled);
        factory.setConnectionTimeout(connectionTimeout);
        factory.setHandshakeTimeout(handshakeTimeout);
        try {
            this.connection = factory.newConnection();
            this.channel = this.connection.createChannel();
            channel.queueDeclare(queueName, false, false, false, null);
        } catch (Exception e) {
            log.error("Failed to connect to RabbitMq");
            throw new RuntimeException(e);
        }

        initConsumer();
    }

    private void initConsumer() {
        executor.submit(() -> {
            while (!STOPPED) {
                if (ackMap.get(collectiveTenantId).get()) {
                    try {
                        int countSize = (int) channel.messageCount(queueName);
                        if (countSize > 0) {
                            ackMap.get(collectiveTenantId).set(false);
                            int currentPackSize = Math.min(countSize, msgPackSize);
                            channel.basicQos(0, currentPackSize, false);

                            UUID packId = UUID.randomUUID();
                            TbMsgQueuePack pack = new TbMsgQueuePack(
                                    packId,
                                    new AtomicInteger(0),
                                    new AtomicInteger(0),
                                    new AtomicInteger(0),
                                    new AtomicBoolean(false),
                                    collectiveTenantId);
                            for (int i = 0; i < currentPackSize; i++) {
                                GetResponse response = channel.basicGet(queueName, false);
                                String stringTenantId = response.getProps().getHeaders().get(TENANT_KEY).toString();
                                TenantId tenantId = new TenantId(UUID.fromString(stringTenantId));
                                TbMsg msg = TbMsg.fromBytes(response.getBody());
                                TbMsgQueueState msgQueueState = new TbMsgQueueState(
                                        msg.copy(msg.getId(), packId, msg.getRuleChainId(), msg.getRuleNodeId(), msg.getClusterPartition()),
                                        tenantId,
                                        new AtomicInteger(0),
                                        new AtomicBoolean(false));
                                pack.addMsg(msgQueueState);
                            }
                            channel.basicAck(0, true);
                            packMap.put(pack.getTenantId(), pack);
                            send(pack);
                        }
                    } catch (IOException e) {
                        log.error("Failed to consume msgs", e);
                    }
                }
            }
        });
    }

    @Override
    public void add(TbMsg msg, TenantId tenantId) {
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put(TENANT_KEY, tenantId.toString());
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().headers(headerMap).build();

        try {
            channel.basicPublish(exchangeName, queueName, properties, TbMsg.toByteArray(msg));
            log.info("Add new message: [{}] for tenant: [{}]", msg, tenantId.getId());
        } catch (IOException e) {
            log.error("Failed to add msg [{}] to RabbitMq", msg, e);
        }
    }

    @Override
    @PreDestroy
    protected void destroy() {
        super.destroy();
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Failed to close connection", e);
            }
        }

        if (channel != null) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                log.error("Failed to close chanel", e);
            }
        }
    }
}
