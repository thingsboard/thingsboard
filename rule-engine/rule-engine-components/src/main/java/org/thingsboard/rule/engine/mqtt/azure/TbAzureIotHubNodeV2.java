/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.rule.engine.mqtt.azure;

import com.microsoft.azure.sdk.iot.service.DeliveryAcknowledgement;
import com.microsoft.azure.sdk.iot.service.IotHubServiceClientProtocol;
import com.microsoft.azure.sdk.iot.service.Message;
import com.microsoft.azure.sdk.iot.service.ServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "azure iot hub V2",
        configClazz = TbAzureIotHubNodeConfigurationV2.class,
        nodeDescription = "Publish messages to the Azure IoT Hub (Recommended)",
        nodeDetails = "Will publish message payload to the Azure IoT Hub with AMQP sender </b>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeAzureIotHubConfigV2"
)
public class TbAzureIotHubNodeV2 implements TbNode {
    private static final String ERROR = "error";
    private TbAzureIotHubNodeConfigurationV2 config;
    private ServiceClient serviceClient;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbAzureIotHubNodeConfigurationV2.class);
        this.config = new TbAzureIotHubNodeConfigurationV2().defaultConfiguration();
        try {
            serviceClient = ServiceClient.createFromConnectionString(this.config.getConnString(), IotHubServiceClientProtocol.AMQPS);
            serviceClient.open();
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        publishMessageAsync(ctx, msg);
    }

    private void publishMessageAsync(TbContext ctx, TbMsg msg) {
        ctx.getExternalCallExecutor().executeAsync(() -> publishMessage(ctx, msg));
    }

    private void publishMessage(TbContext ctx, TbMsg msg) {
        try {
            Message message = new Message(msg.getData());
            message.setDeliveryAcknowledgement(DeliveryAcknowledgement.Full);
            message.setMessageId(UUID.randomUUID().toString());
            message.getProperties().put("content-type", "JSON");
            CompletableFuture<Void> future = serviceClient.sendAsync(config.getDeviceId(), message);
            future.whenCompleteAsync((success, err) -> {
                if (err != null) {
                    TbMsg next = processException(ctx, msg, err);
                    ctx.tellFailure(next, err);
                } else {
                    ctx.tellSuccess(msg);
                }
            }, Executors.newCachedThreadPool());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        try {
            serviceClient.close();
        } catch (IOException e) {
        }
        TbNode.super.destroy();
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        TbNode.super.onPartitionChangeMsg(ctx, msg);
    }

    private TbMsg processException(TbContext ctx, TbMsg origMsg, Throwable t) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, t.getClass() + ": " + t.getMessage());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }
}

