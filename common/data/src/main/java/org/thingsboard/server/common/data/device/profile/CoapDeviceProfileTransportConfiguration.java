// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.PowerSavingConfiguration;

@Data
public class CoapDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    private CoapDeviceTypeConfiguration coapDeviceTypeConfiguration;
    private PowerSavingConfiguration clientSettings;

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.COAP;
    }

    public CoapDeviceTypeConfiguration getCoapDeviceTypeConfiguration() {
        if (coapDeviceTypeConfiguration != null) {
            return coapDeviceTypeConfiguration;
        } else {
            return new DefaultCoapDeviceTypeConfiguration();
        }
    }
}