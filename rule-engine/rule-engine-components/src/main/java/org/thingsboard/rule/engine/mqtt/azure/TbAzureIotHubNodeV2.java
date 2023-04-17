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

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageSentCallback;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubClientException;
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


@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "azure iot hub",
        configClazz = TbAzureIotHubNodeConfigurationV2.class,
        nodeDescription = "Publish messages to the Azure IoT Hub (Recommended)",
        nodeDetails = "Will publish message payload to the Azure IoT Hub with AMQP sender </b>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeAzureIotHubConfigV2"
)
public class TbAzureIotHubNodeV2 implements TbNode {
    private static final String ERROR = "error";
    private TbAzureIotHubNodeConfigurationV2 config;
    private DeviceClient client;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbAzureIotHubNodeConfigurationV2.class);
        try {
            client = new DeviceClient(this.config.getDeviceConnString(), IotHubClientProtocol.AMQPS);
            client.open(true);
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg)  {
        Message message = new Message(msg.getData());
        message.setContentType(msg.getDataType().name());
        message.setMessageId(java.util.UUID.randomUUID().toString());

        MessageSentCallback messageSentCallback = new MessageSentCallback() {
            @Override
            public void onMessageSent(Message message, IotHubClientException e, Object o) {
                if (e != null) {
                    TbMsg next = processException(ctx, msg, e);
                    ctx.tellFailure(next, e);
                } else {
                    ctx.tellSuccess(msg);
                }
            }
        };
        client.sendEventAsync(message, messageSentCallback , null);
    }

    @Override
    public void destroy() {
        if (this.client != null) {
            try {
                this.client.close();
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

