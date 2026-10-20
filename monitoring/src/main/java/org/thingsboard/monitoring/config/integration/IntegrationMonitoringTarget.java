// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.monitoring.config.integration;

import com.google.common.base.Strings;
import lombok.Data;
import org.thingsboard.monitoring.config.MonitoringTarget;
import org.thingsboard.monitoring.config.transport.DeviceConfig;
import org.thingsboard.server.common.data.integration.Integration;

import java.util.UUID;

@Data
public class IntegrationMonitoringTarget implements MonitoringTarget {

    private String baseUrl;
    private DeviceConfig device; // set manually during initialization
    private Integration integration; // set manually during initialization
    private boolean checkDomainIps;
    private String namePrefix;

    @Override
    public UUID getDeviceId() {
        return device.getId();
    }

    public String getNamePrefix() {
        return Strings.nullToEmpty(namePrefix);
    }

}
