// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import org.thingsboard.server.common.data.CoapDeviceType;

@Data
public class DefaultCoapDeviceTypeConfiguration implements CoapDeviceTypeConfiguration {

    private static final long serialVersionUID = -4287100699186773773L;

    private TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;

    @Override
    public CoapDeviceType getCoapDeviceType() {
        return CoapDeviceType.DEFAULT;
    }

    public TransportPayloadTypeConfiguration getTransportPayloadTypeConfiguration() {
        if (transportPayloadTypeConfiguration != null) {
            return transportPayloadTypeConfiguration;
        } else {
            return new JsonTransportPayloadConfiguration();
        }
    }

}
