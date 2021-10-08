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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create device",
        configClazz = TbCreateDeviceNodeConfiguration.class,
        nodeDescription = "Create or update device.",
        nodeDetails = "Details - to create the device should be indicated device name and type, otherwise message send via " +
                "<b>Failure</b> chain. If the type is not selected - it will be save as <b>default<b>" +
                "If the device already exists or successfully created -  Message send via <b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "",
        icon = "add_circle"
)
public class TbCreateDeviceNode implements TbNode {

    private TbCreateDeviceNodeConfiguration config;
    private String deviceName;
    private String deviceType;
    private String accessToken;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCreateDeviceNodeConfiguration.class);
        this.deviceName = config.getName();
        this.deviceType = config.getType();
        this.accessToken = config.getAccessToken();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (StringUtils.isEmpty(deviceName)) {
            ctx.tellFailure(msg, new IllegalArgumentException("Device name is null or empty "));
        } else {
            try {
                deviceName = TbNodeUtils.processPattern(deviceName, msg);
                deviceType = setDeviceType(msg);
                checkRegexValidation(deviceName, deviceType);
                Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(ctx.getTenantId(), deviceName);
                if (device != null) {
                    if (!device.getType().equals(deviceType)) {
                        device.setType(deviceType);
                        device.setDeviceProfileId(null);
                    }
                    DeviceCredentials deviceCredentials = ctx.deviceCredentialsService().findDeviceCredentialsByDeviceId(ctx.getTenantId(), device.getId());
                    if (accessToken.equals(config.getAccessToken())) {
                        accessToken = replaceRegex(accessToken);
                    }
                    deviceCredentials.setCredentialsId(processPatternAccessToken(msg));
                    ctx.deviceCredentialsService().updateDeviceCredentials(ctx.getTenantId(), deviceCredentials);
                } else {
                    device = new Device();
                    device.setTenantId(ctx.getTenantId());
                }
                createOrUpdateDeviceInfo(device, msg);
                ctx.getDeviceService().saveDeviceWithAccessToken(device, processPatternAccessToken(msg));

                ctx.tellSuccess(msg);
            } catch (Exception e) {
                ctx.tellFailure(msg, e);
            }
        }
    }

    @Override
    public void destroy() {

    }

    private void createOrUpdateDeviceInfo(Device device, TbMsg msg) {
        device.setName(deviceName);
        device.setType(deviceType);
        if (!StringUtils.isEmpty(config.getLabel())) {
            device.setLabel(TbNodeUtils.processPattern(config.getLabel(), msg));
        }
        device.setAdditionalInfo(createAdditionalInfo(device));
    }

    private String processPatternAccessToken(TbMsg msg) {
        return Objects.requireNonNullElse(TbNodeUtils.processPattern(accessToken, msg), "");
    }

    private String setDeviceType(TbMsg msg) {
        if (StringUtils.isEmpty(deviceType)) {
            return "default";
        } else {
            return TbNodeUtils.processPattern(config.getType(), msg);
        }
    }

    private JsonNode createAdditionalInfo(Device device) {
        JsonNode additionalInfo = device.getAdditionalInfo();
        ObjectNode additionalInfoObjNode;
        if (additionalInfo == null) {
            additionalInfoObjNode = JacksonUtil.newObjectNode();
        } else {
            additionalInfoObjNode = (ObjectNode) additionalInfo;
        }
        additionalInfoObjNode.put("gateway", config.isGateway());
        additionalInfoObjNode.put("overwriteActivityTime", config.isOverwriteActivityTime());
        additionalInfoObjNode.put("description", config.getDescription());
        return additionalInfoObjNode;
    }

    private void checkRegexValidation(String name, String type) {
        if (type.equals(config.getType())) {
            deviceType = replaceRegex(type);
        }
        if (name.equals(config.getName())) {
            deviceName = replaceRegex(name);
        }
    }

    private String replaceRegex(String pattern) {
        return pattern.replaceAll("\\$\\{?\\[?", "").replaceAll("}?]?", "");
    }

}
