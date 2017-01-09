/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.rabbitmq.plugin;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.rabbitmq.action.RabbitMqPluginAction;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Plugin(name = "RabbitMQ Plugin", actions = {RabbitMqPluginAction.class},
descriptor = "RabbitMqPluginDescriptor.json", configuration = RabbitMqPluginConfiguration.class)
@Slf4j
public class RabbitMqPlugin extends AbstractPlugin<RabbitMqPluginConfiguration> {

    private ConnectionFactory factory;
    private Connection connection;
    private RabbitMqMsgHandler handler;

    @Override
    public void init(RabbitMqPluginConfiguration configuration) {
        factory = new ConnectionFactory();
        factory.setHost(configuration.getHost());
        factory.setPort(configuration.getPort());
        set(configuration.getVirtualHost(), factory::setVirtualHost);
        set(configuration.getUserName(), factory::setUsername);
        set(configuration.getPassword(), factory::setPassword);
        set(configuration.getAutomaticRecoveryEnabled(), factory::setAutomaticRecoveryEnabled);
        set(configuration.getConnectionTimeout(), factory::setConnectionTimeout);
        set(configuration.getHandshakeTimeout(), factory::setHandshakeTimeout);
        set(configuration.getClientProperties(), props -> {
            factory.setClientProperties(props.stream().collect(Collectors.toMap(
                    RabbitMqPluginConfiguration.RabbitMqPluginProperties::getKey,
                    RabbitMqPluginConfiguration.RabbitMqPluginProperties::getValue)));
        });

        init();
    }

    private <T> void set(T source, Consumer<T> setter) {
        if (source != null && !StringUtils.isEmpty(source.toString())) {
            setter.accept(source);
        }
    }

    private void init() {
        try {
            this.connection = factory.newConnection();
            this.handler = new RabbitMqMsgHandler(connection.createChannel());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private void destroy() {
        try {
            this.handler = null;
            this.connection.close();
        } catch (Exception e) {
            log.info("Failed to close connection during destroy()", e);
        }
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return handler;
    }

    @Override
    public void resume(PluginContext ctx) {
        init();
    }

    @Override
    public void suspend(PluginContext ctx) {
        destroy();
    }

    @Override
    public void stop(PluginContext ctx) {
        destroy();
    }

}
