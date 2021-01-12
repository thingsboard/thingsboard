/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.queue.settings;

import com.rabbitmq.client.ConnectionFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq'")
@Component
@Data
public class TbRabbitMqSettings {
    @Value("${queue.rabbitmq.exchange_name:}")
    private String exchangeName;
    @Value("${queue.rabbitmq.host:}")
    private String host;
    @Value("${queue.rabbitmq.port:}")
    private int port;
    @Value("${queue.rabbitmq.virtual_host:}")
    private String virtualHost;
    @Value("${queue.rabbitmq.username:}")
    private String username;
    @Value("${queue.rabbitmq.password:}")
    private String password;
    @Value("${queue.rabbitmq.automatic_recovery_enabled:}")
    private boolean automaticRecoveryEnabled;
    @Value("${queue.rabbitmq.connection_timeout:}")
    private int connectionTimeout;
    @Value("${queue.rabbitmq.handshake_timeout:}")
    private int handshakeTimeout;

    private ConnectionFactory connectionFactory;

    @PostConstruct
    private void init() {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setAutomaticRecoveryEnabled(automaticRecoveryEnabled);
        connectionFactory.setConnectionTimeout(connectionTimeout);
        connectionFactory.setHandshakeTimeout(handshakeTimeout);
    }
}
