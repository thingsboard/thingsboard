/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.rule.engine.rabbitmq;

import com.google.common.util.concurrent.ListenableFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "rabbitmq",
        configClazz = TbRabbitMqNodeConfiguration.class,
        nodeDescription = "Publish messages to the RabbitMQ",
        nodeDetails = "Will publish message payload to RabbitMQ queue.",
        configDirective = "tbExternalNodeRabbitMqConfig",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbDpzcGFjZT0icHJlc2VydmUiIHZlcnNpb249IjEuMSIgeT0iMHB4IiB4PSIwcHgiIHZpZXdCb3g9IjAgMCAxMDAwIDEwMDAiPjxwYXRoIHN0cm9rZS13aWR0aD0iLjg0OTU2IiBkPSJtODYwLjQ3IDQxNi4zMmgtMjYyLjAxYy0xMi45MTMgMC0yMy42MTgtMTAuNzA0LTIzLjYxOC0yMy42MTh2LTI3Mi43MWMwLTIwLjMwNS0xNi4yMjctMzYuMjc2LTM2LjI3Ni0zNi4yNzZoLTkzLjc5MmMtMjAuMzA1IDAtMzYuMjc2IDE2LjIyNy0zNi4yNzYgMzYuMjc2djI3MC44NGMtMC4yNTQ4NyAxNC4xMDMtMTEuNDY5IDI1LjU3Mi0yNS43NDIgMjUuNTcybC04NS42MzYgMC42Nzk2NWMtMTQuMTAzIDAtMjUuNTcyLTExLjQ2OS0yNS41NzItMjUuNTcybDAuNjc5NjUtMjcxLjUyYzAtMjAuMzA1LTE2LjIyNy0zNi4yNzYtMzYuMjc2LTM2LjI3NmgtOTMuNTM3Yy0yMC4zMDUgMC0zNi4yNzYgMTYuMjI3LTM2LjI3NiAzNi4yNzZ2NzYzLjg0YzAgMTguMDk2IDE0Ljc4MiAzMi40NTMgMzIuNDUzIDMyLjQ1M2g3MjIuODFjMTguMDk2IDAgMzIuNDUzLTE0Ljc4MiAzMi40NTMtMzIuNDUzdi00MzUuMzFjLTEuMTg5NC0xOC4xODEtMTUuMjkyLTMyLjE5OC0zMy4zODgtMzIuMTk4em0tMTIyLjY4IDI4Ny4wN2MwIDIzLjYxOC0xOC44NiA0Mi40NzgtNDIuNDc4IDQyLjQ3OGgtNzMuOTk3Yy0yMy42MTggMC00Mi40NzgtMTguODYtNDIuNDc4LTQyLjQ3OHYtNzQuMjUyYzAtMjMuNjE4IDE4Ljg2LTQyLjQ3OCA0Mi40NzgtNDIuNDc4aDczLjk5N2MyMy42MTggMCA0Mi40NzggMTguODYgNDIuNDc4IDQyLjQ3OHoiLz48L3N2Zz4=",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/external/rabbitmq/"
)
public class TbRabbitMqNode extends TbAbstractExternalNode {

    private static final String supportedPropertiesStr = String.join(", ",
            "BASIC", "TEXT_PLAIN", "MINIMAL_BASIC", "MINIMAL_PERSISTENT_BASIC", "PERSISTENT_BASIC", "PERSISTENT_TEXT_PLAIN"
    );

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private static final String ERROR = "error";

    private TbRabbitMqNodeConfiguration config;

    private Connection connection;
    private Channel channel;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.config = TbNodeUtils.convert(configuration, TbRabbitMqNodeConfiguration.class);
        ConnectionFactory factory = getConnectionFactory();
        try {
            this.connection = factory.newConnection();
            this.channel = this.connection.createChannel();
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var tbMsg = ackIfNeeded(ctx, msg);
        withCallback(publishMessageAsync(ctx, tbMsg),
                m -> tellSuccess(ctx, m),
                t -> tellFailure(ctx, processException(tbMsg, t), t));
    }

    ConnectionFactory getConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.config.getHost());
        factory.setPort(this.config.getPort());
        factory.setVirtualHost(this.config.getVirtualHost());
        factory.setUsername(this.config.getUsername());
        factory.setPassword(this.config.getPassword());
        factory.setAutomaticRecoveryEnabled(this.config.isAutomaticRecoveryEnabled());
        factory.setConnectionTimeout(this.config.getConnectionTimeout());
        factory.setHandshakeTimeout(this.config.getHandshakeTimeout());
        this.config.getClientProperties().forEach((k, v) -> factory.getClientProperties().put(k, v));
        return factory;
    }

    private ListenableFuture<TbMsg> publishMessageAsync(TbContext ctx, TbMsg msg) {
        return ctx.getExternalCallExecutor().executeAsync(() -> publishMessage(msg));
    }

    private TbMsg publishMessage(TbMsg msg) throws Exception {
        String exchangeName = "";
        if (!StringUtils.isEmpty(this.config.getExchangeNamePattern())) {
            exchangeName = TbNodeUtils.processPattern(this.config.getExchangeNamePattern(), msg);
        }
        String routingKey = "";
        if (!StringUtils.isEmpty(this.config.getRoutingKeyPattern())) {
            routingKey = TbNodeUtils.processPattern(this.config.getRoutingKeyPattern(), msg);
        }
        AMQP.BasicProperties properties = null;
        if (!StringUtils.isEmpty(this.config.getMessageProperties())) {
            properties = convert(this.config.getMessageProperties());
        }
        channel.basicPublish(
                exchangeName,
                routingKey,
                properties,
                msg.getData().getBytes(UTF8));
        return msg;
    }

    private TbMsg processException(TbMsg origMsg, Throwable t) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, t.getClass() + ": " + t.getMessage());
        return origMsg.transform()
                .metaData(metaData)
                .build();
    }

    @Override
    public void destroy() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (Exception e) {
                log.error("Failed to close connection during destroy()", e);
            }
        }
    }

    static AMQP.BasicProperties convert(String name) throws TbNodeException {
        return switch (name) {
            case "BASIC" -> MessageProperties.BASIC;
            case "TEXT_PLAIN" -> MessageProperties.TEXT_PLAIN;
            case "MINIMAL_BASIC" -> MessageProperties.MINIMAL_BASIC;
            case "MINIMAL_PERSISTENT_BASIC" -> MessageProperties.MINIMAL_PERSISTENT_BASIC;
            case "PERSISTENT_BASIC" -> MessageProperties.PERSISTENT_BASIC;
            case "PERSISTENT_TEXT_PLAIN" -> MessageProperties.PERSISTENT_TEXT_PLAIN;
            default -> throw new TbNodeException("Undefined message properties type '" + name + "'! Only " + supportedPropertiesStr + " message properties types are supported!");
        };
    }

}
