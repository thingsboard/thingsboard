// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.prototypes;

import com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;

public class DevicePrototypes {
    public static Device defaultDevicePrototype(String name){
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        return device;
    }

    public static Device defaultGatewayPrototype() {
        String isGateway = "{\"gateway\":true}";
        JsonNode additionalInfo = JacksonUtil.toJsonNode(isGateway);
        Device gatewayDeviceTemplate = new Device();
        gatewayDeviceTemplate.setName("mqtt_gateway_" + RandomStringUtils.randomAlphanumeric(5));
        gatewayDeviceTemplate.setType("gateway");
        gatewayDeviceTemplate.setAdditionalInfo(additionalInfo);
        return gatewayDeviceTemplate;
    }
}
