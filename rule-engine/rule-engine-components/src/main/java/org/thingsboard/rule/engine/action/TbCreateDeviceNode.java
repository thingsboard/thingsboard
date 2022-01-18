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
package org.thingsboard.rule.engine.action;


import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "get or create device",
        configClazz = TbCreateDeviceNodeConfiguration.class,
        nodeDescription = "Get or Create device based on selected configuration",
        nodeDetails = "Try to find target device by <b>Name pattern</b> or create device if it doesn't exists. In both cases incoming message send via <b>Success</b> chain.</br>" +
                "In case that device already exists, a message with device entity as message originator and msg type <b>DEVICE_FETCHED</b> will be generated.</br>" +
                "In case that device doesn't exists, rule node will create a device based on selected configuration and generate a message with device entity as message originator and msg type <b>DEVICE_CREATED</b>.</br>" +
                "Additionally <b>ENTITY_CREATED</b> event will generate and push to Root Rule Chain or to rule chain selected in the device profile</br>" +
                "In both cases for message with type <b>DEVICE_CREATED</b> and type <b>DEVICE_FETCHED</b> message content will be not changed from initial incoming message.</br>" +
                "In case that <b>Name pattern</b> will be not specified or any processing errors will occurred the result message send via <b>Failure</b> chain.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeSaveDeviceConfig",
        icon = "add_circle"
)
public class TbCreateDeviceNode extends TbAbstractCreateEntityNode<TbCreateDeviceNodeConfiguration> {

    @Override
    protected TbCreateDeviceNodeConfiguration initConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbCreateDeviceNodeConfiguration.class);
    }

    @Override
    protected void processOnMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        if (StringUtils.isEmpty(name)) {
            ctx.tellFailure(msg, new IllegalArgumentException("Device name is null or empty!"));
        } else {
            try {
                String deviceName = TbNodeUtils.processPattern(name, msg);
                Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(ctx.getTenantId(), deviceName);
                if (device == null) {
                    Device savedDevice = createDevice(ctx, msg, deviceName);
                    ctx.getClusterService().onDeviceUpdated(savedDevice, null);
                    ctx.enqueue(ctx.deviceCreatedMsg(savedDevice, ctx.getSelfId()),
                            () -> log.trace("Pushed Device Created message: {}", savedDevice),
                            throwable -> log.warn("Failed to push Device Created message: {}", savedDevice, throwable));
                    ctx.transformMsg(msg, DataConstants.DEVICE_CREATED, savedDevice.getId(), msg.getMetaData(), msg.getData());
                } else {
                    ctx.transformMsg(msg, DataConstants.DEVICE_FETCHED, device.getId(), msg.getMetaData(), msg.getData());
                }
                ctx.tellSuccess(msg);
            } catch (Exception e) {
                ctx.tellFailure(msg, e);
            }
        }
    }

    private Device createDevice(TbContext ctx, TbMsg msg, String name) {
        Device device = new Device();
        device.setName(name);
        if (!StringUtils.isEmpty(type)) {
            device.setType(TbNodeUtils.processPattern(type, msg));
        }
        if (!StringUtils.isEmpty(label)) {
            device.setLabel(TbNodeUtils.processPattern(label, msg));
        }
        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        if (!StringUtils.isEmpty(description)) {
            additionalInfo.put("description", TbNodeUtils.processPattern(description, msg));
        }
        if (config.isGateway()) {
            additionalInfo.put("gateway", config.isGateway());
            if (config.isOverwriteActivityTime()) {
                additionalInfo.put("overwriteActivityTime", config.isOverwriteActivityTime());
            }
        }
        device.setAdditionalInfo(additionalInfo);
        return ctx.getDeviceService().saveDevice(device);
    }
}
