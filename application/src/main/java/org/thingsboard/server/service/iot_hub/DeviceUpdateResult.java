// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.iot_hub.DeviceInstalledItemDescriptor;

import java.util.Map;

/**
 * Either everything was applied, or nothing was: on a failure the transaction was rolled back and failedStep
 * is the install state key of the step that failed, if one did.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceUpdateResult {

    private boolean success;
    private String errorMessage;
    private String failedStep;
    private DeviceInstalledItemDescriptor descriptor;
    // By install state key of the step: what the install instructions resolve, e.g. ${integration.httpEndpoint}, ${device.token}
    private Map<String, ObjectNode> outputs;

    public static DeviceUpdateResult success(DeviceInstalledItemDescriptor descriptor, Map<String, ObjectNode> outputs) {
        return new DeviceUpdateResult(true, null, null, descriptor, outputs);
    }

    public static DeviceUpdateResult error(String errorMessage, String failedStep) {
        return new DeviceUpdateResult(false, errorMessage, failedStep, null, null);
    }

}
