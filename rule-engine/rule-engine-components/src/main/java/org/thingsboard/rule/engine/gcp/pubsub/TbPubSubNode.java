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
package org.thingsboard.rule.engine.gcp.pubsub;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "gcp pubsub",
        configClazz = TbPubSubNodeConfiguration.class,
        nodeDescription = "Publish message to the Google Cloud PubSub",
        nodeDetails = "Will publish message payload to the Google Cloud Platform PubSub topic. Outbound message will contain response fields " +
                "(<code>messageId</code> in the Message Metadata from the GCP PubSub. " +
                "<b>messageId</b> field can be accessed with <code>metadata.messageId</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodePubSubConfig",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyBpZD0iTGF5ZXJfMSIgZGF0YS1uYW1lPSJMYXllciAxIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMTI4IDEyOCI+Cjx0aXRsZT5DbG91ZCBQdWJTdWI8L3RpdGxlPgo8Zz4KPHBhdGggZD0iTTEyNi40Nyw1OC4xMmwtMjYuMy00NS43NEExMS41NiwxMS41NiwwLDAsMCw5MC4zMSw2LjVIMzcuN2ExMS41NSwxMS41NSwwLDAsMC05Ljg2LDUuODhMMS41Myw1OGExMS40OCwxMS40OCwwLDAsMCwwLDExLjQ0bDI2LjMsNDZhMTEuNzcsMTEuNzcsMCwwLDAsOS44Niw2LjA5SDkwLjNhMTEuNzMsMTEuNzMsMCwwLDAsOS44Ny02LjA2bDI2LjMtNDUuNzRBMTEuNzMsMTEuNzMsMCwwLDAsMTI2LjQ3LDU4LjEyWiIgc3R5bGU9ImZpbGw6ICM3MzViMmYiLz4KPHBhdGggZD0iTTg5LjIyLDQ3Ljc0LDgzLjM2LDQ5bC0xNC42LTE0LjZMNjQuMDksNDMuMSw2MS41NSw1My4ybDQuMjksNC4yOUw1Ny42LDU5LjE4LDQ2LjMsNDcuODhsLTcuNjcsNy4zOEw1Mi43Niw2OS4zN2wtMTUsMTEuOUw3OCwxMjEuNUg5MC4zYTExLjczLDExLjczLDAsMCwwLDkuODctNi4wNmwyMC43Mi0zNloiIHN0eWxlPSJvcGFjaXR5OiAwLjA3MDAwMDAwMDI5ODAyMztpc29sYXRpb246IGlzb2xhdGUiLz4KPHBhdGggZD0iTTgyLjg2LDQ3YTUuMzIsNS4zMiwwLDEsMS0xLjk1LDcuMjdBNS4zMiw1LjMyLDAsMCwxLDgyLjg2LDQ3IiBzdHlsZT0iZmlsbDogI2ZmZiIvPgo8cGF0aCBkPSJNMzkuODIsNTYuMThhNS4zMiw1LjMyLDAsMSwxLDcuMjctMS45NSw1LjMyLDUuMzIsMCwwLDEtNy4yNywxLjk1IiBzdHlsZT0iZmlsbDogI2ZmZiIvPgo8cGF0aCBkPSJNNjkuMzIsODguODVBNS4zMiw1LjMyLDAsMSwxLDY0LDgzLjUyYTUuMzIsNS4zMiwwLDAsMSw1LjMyLDUuMzIiIHN0eWxlPSJmaWxsOiAjZmZmIi8+CjxnPgo8cGF0aCBkPSJNNjQsNTIuOTRhMTEuMDYsMTEuMDYsMCwwLDEsMi40Ni4yOFYzOS4xNUg2MS41NFY1My4yMkExMS4wNiwxMS4wNiwwLDAsMSw2NCw1Mi45NFoiIHN0eWxlPSJmaWxsOiAjZmZmIi8+CjxwYXRoIGQ9Ik03NC41Nyw2Ny4yNmExMSwxMSwwLDAsMS0yLjQ3LDQuMjVsMTIuMTksNywyLjQ2LTQuMjZaIiBzdHlsZT0iZmlsbDogI2ZmZiIvPgo8cGF0aCBkPSJNNTMuNDMsNjcuMjZsLTEyLjE4LDcsMi40Niw0LjI2LDEyLjE5LTdBMTEsMTEsMCwwLDEsNTMuNDMsNjcuMjZaIiBzdHlsZT0iZmlsbDogI2ZmZiIvPgo8L2c+CjxwYXRoIGQ9Ik03Mi42LDY0QTguNiw4LjYsMCwxLDEsNjQsNTUuNCw4LjYsOC42LDAsMCwxLDcyLjYsNjQiIHN0eWxlPSJmaWxsOiAjZmZmIi8+CjxwYXRoIGQ9Ik0zOS4xLDcwLjU3YTYuNzYsNi43NiwwLDEsMS0yLjQ3LDkuMjMsNi43Niw2Ljc2LDAsMCwxLDIuNDctOS4yMyIgc3R5bGU9ImZpbGw6ICNmZmYiLz4KPHBhdGggZD0iTTgyLjE0LDgyLjI3YTYuNzYsNi43NiwwLDEsMSw5LjIzLTIuNDcsNi43NSw2Ljc1LDAsMCwxLTkuMjMsMi40NyIgc3R5bGU9ImZpbGw6ICNmZmYiLz4KPHBhdGggZD0iTTcwLjc2LDM5LjE1QTYuNzYsNi43NiwwLDEsMSw2NCwzMi4zOWE2Ljc2LDYuNzYsMCwwLDEsNi43Niw2Ljc2IiBzdHlsZT0iZmlsbDogI2ZmZiIvPgo8L2c+Cjwvc3ZnPgo="
)
public class TbPubSubNode implements TbNode {

    private static final String MESSAGE_ID = "messageId";
    private static final String ERROR = "error";

    private TbPubSubNodeConfiguration config;
    private Publisher pubSubClient;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        try {
            this.config = TbNodeUtils.convert(configuration, TbPubSubNodeConfiguration.class);
            this.pubSubClient = initPubSubClient();
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        publishMessage(ctx, msg);
    }

    @Override
    public void destroy() {
        if (this.pubSubClient != null) {
            try {
                this.pubSubClient.shutdown();
                this.pubSubClient.awaitTermination(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Failed to shutdown PubSub client during destroy()", e);
            }
        }
    }

    private void publishMessage(TbContext ctx, TbMsg msg) {
        ByteString data = ByteString.copyFromUtf8(msg.getData());
        PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder();
        pubsubMessageBuilder.setData(data);
        this.config.getMessageAttributes().forEach((k, v) -> {
            String name = TbNodeUtils.processPattern(k, msg.getMetaData());
            String val = TbNodeUtils.processPattern(v, msg.getMetaData());
            pubsubMessageBuilder.putAttributes(name, val);
        });
        ApiFuture<String> messageIdFuture = this.pubSubClient.publish(pubsubMessageBuilder.build());
        ApiFutures.addCallback(messageIdFuture, new ApiFutureCallback<String>() {
                    public void onSuccess(String messageId) {
                        TbMsg next = processPublishResult(ctx, msg, messageId);
                        ctx.tellSuccess(next);
                    }

                    public void onFailure(Throwable t) {
                        TbMsg next = processException(ctx, msg, t);
                        ctx.tellFailure(next, t);
                    }
                },
                ctx.getExternalCallExecutor());
    }

    private TbMsg processPublishResult(TbContext ctx, TbMsg origMsg, String messageId) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(MESSAGE_ID, messageId);
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    private TbMsg processException(TbContext ctx, TbMsg origMsg, Throwable t) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, t.getClass() + ": " + t.getMessage());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    private Publisher initPubSubClient() throws IOException {
        ProjectTopicName topicName = ProjectTopicName.of(config.getProjectId(), config.getTopicName());
        ServiceAccountCredentials credentials =
                ServiceAccountCredentials.fromStream(
                        new ByteArrayInputStream(config.getServiceAccountKey().getBytes()));
        CredentialsProvider credProvider = FixedCredentialsProvider.create(credentials);

        return Publisher.newBuilder(topicName)
                .setCredentialsProvider(credProvider)
                .build();
    }
}
