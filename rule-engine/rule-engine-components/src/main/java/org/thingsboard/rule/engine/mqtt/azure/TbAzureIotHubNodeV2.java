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
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
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
    private ExecutorService executorService;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        executorService = Executors.newCachedThreadPool();
        this.config = TbNodeUtils.convert(configuration, TbAzureIotHubNodeConfigurationV2.class);
        try {
            serviceClient = ServiceClient.createFromConnectionString(this.config.getConnString(), IotHubServiceClientProtocol.AMQPS);
            serviceClient.open();
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg)  {
        try {
            Message message = new Message(msg.getData());
            message.setDeliveryAcknowledgement(DeliveryAcknowledgement.Full);
            message.setMessageId(UUID.randomUUID().toString());
            message.getProperties().put("content-type", msg.getDataType().name());

            CompletableFuture<Void> future = serviceClient.sendAsync(config.getDeviceId(), message);
            future.whenCompleteAsync((success, err) -> {
                if (err != null) {
                    TbMsg next = processException(ctx, msg, err);
                    ctx.tellFailure(next, err);
                } else {
                    ctx.tellSuccess(msg);
                }
            }, executorService);
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to create message ", e);
        }
    }

    @Override
    public void destroy() {
        if (this.serviceClient != null) {
            try {
                this.serviceClient.close();
            } catch (Exception e) {
                log.error("Failed to close Azure iot hub client during destroy()", e);
            }
        }
    }

    private TbMsg processException(TbContext ctx, TbMsg origMsg, Throwable t) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, t.getClass() + ": " + t.getMessage());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }
}

