// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.Map;

@Data
public class TbRabbitMqNodeConfiguration implements NodeConfiguration<TbRabbitMqNodeConfiguration> {

    private String exchangeNamePattern;
    private String routingKeyPattern;
    private String messageProperties;
    private String host;
    private int port;
    private String virtualHost;
    private String username;
    private String password;
    private boolean automaticRecoveryEnabled;
    private int connectionTimeout;
    private int handshakeTimeout;
    private Map<String, String> clientProperties;

    @Override
    public TbRabbitMqNodeConfiguration defaultConfiguration() {
        TbRabbitMqNodeConfiguration configuration = new TbRabbitMqNodeConfiguration();
        configuration.setExchangeNamePattern("");
        configuration.setRoutingKeyPattern("");
        configuration.setMessageProperties(null);
        configuration.setPort(ConnectionFactory.DEFAULT_AMQP_PORT);
        configuration.setVirtualHost(ConnectionFactory.DEFAULT_VHOST);
        configuration.setUsername(ConnectionFactory.DEFAULT_USER);
        configuration.setPassword(ConnectionFactory.DEFAULT_PASS);
        configuration.setAutomaticRecoveryEnabled(false);
        configuration.setConnectionTimeout(ConnectionFactory.DEFAULT_CONNECTION_TIMEOUT);
        configuration.setHandshakeTimeout(ConnectionFactory.DEFAULT_HANDSHAKE_TIMEOUT);
        configuration.setClientProperties(Collections.emptyMap());
        return configuration;
    }
}
