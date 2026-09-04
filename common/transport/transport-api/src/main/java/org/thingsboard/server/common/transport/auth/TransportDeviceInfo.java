// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth;

import lombok.Data;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

@Data
public class TransportDeviceInfo implements Serializable {

    private TenantId tenantId;
    private CustomerId customerId;
    private DeviceProfileId deviceProfileId;
    private DeviceId deviceId;
    private String deviceName;
    private String deviceType;
    private PowerMode powerMode;
    private String additionalInfo;
    private Long edrxCycle;
    private Long psmActivityTimer;
    private Long pagingTransmissionWindow;
    private boolean gateway;
}
