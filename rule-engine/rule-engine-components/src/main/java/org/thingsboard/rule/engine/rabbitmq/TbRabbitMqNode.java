/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.rule.engine.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "rabbitmq",
        configClazz = TbRabbitMqNodeConfiguration.class,
        nodeDescription = "Publish messages to RabbitMQ",
        nodeDetails = "Expects messages with any message type. Will publish message to RabbitMQ queue.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeRabbitMqConfig"
)
public class TbRabbitMqNode implements TbNode {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String ERROR = "error";

    private TbRabbitMqNodeConfiguration config;

    private Connection connection;
    private Channel channel;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbRabbitMqNodeConfiguration.class);
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.config.getHost());
        factory.setPort(this.config.getPort());
        factory.setVirtualHost(this.config.getVirtualHost());
        factory.setUsername(this.config.getUsername());
        factory.setPassword(this.config.getPassword());
        factory.setAutomaticRecoveryEnabled(this.config.isAutomaticRecoveryEnabled());
        factory.setConnectionTimeout(this.config.getConnectionTimeout());
        factory.setHandshakeTimeout(this.config.getHandshakeTimeout());
        this.config.getClientProperties().forEach((k,v) -> factory.getClientProperties().put(k,v));
        try {
            this.connection = factory.newConnection();
            this.channel = this.connection.createChannel();
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        withCallback(publishMessageAsync(ctx, msg),
                m -> ctx.tellNext(m, TbRelationTypes.SUCCESS),
                t -> {
                    TbMsg next = processException(ctx, msg, t);
                    ctx.tellNext(next, TbRelationTypes.FAILURE, t);
                });
    }

    ListenableFuture<TbMsg> publishMessageAsync(TbContext ctx, TbMsg msg) {
        return ctx.getExternalCallExecutor().executeAsync(() -> publishMessage(ctx, msg));
    }

    TbMsg publishMessage(TbContext ctx, TbMsg msg) throws Exception {
        String exchangeName = "";
        if (!StringUtils.isEmpty(this.config.getExchangeNamePattern())) {
            exchangeName = TbNodeUtils.processPattern(this.config.getExchangeNamePattern(), msg.getMetaData());
        }
        String routingKey = "";
        if (!StringUtils.isEmpty(this.config.getRoutingKeyPattern())) {
            routingKey = TbNodeUtils.processPattern(this.config.getRoutingKeyPattern(), msg.getMetaData());
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
        return ctx.transformMsg(msg, msg.getType(), msg.getOriginator(), msg.getMetaData(), msg.getData());
    }

    private TbMsg processException(TbContext ctx, TbMsg origMsg, Throwable t) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, t.getClass() + ": " + t.getMessage());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
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

    private static AMQP.BasicProperties convert(String name) throws TbNodeException {
        switch (name) {
            case "BASIC":
                return MessageProperties.BASIC;
            case "TEXT_PLAIN":
                return MessageProperties.TEXT_PLAIN;
            case "MINIMAL_BASIC":
                return MessageProperties.MINIMAL_BASIC;
            case "MINIMAL_PERSISTENT_BASIC":
                return MessageProperties.MINIMAL_PERSISTENT_BASIC;
            case "PERSISTENT_BASIC":
                return MessageProperties.PERSISTENT_BASIC;
            case "PERSISTENT_TEXT_PLAIN":
                return MessageProperties.PERSISTENT_TEXT_PLAIN;
            default:
                throw new TbNodeException("Message Properties: '" + name + "' is undefined!");
        }
    }
}

