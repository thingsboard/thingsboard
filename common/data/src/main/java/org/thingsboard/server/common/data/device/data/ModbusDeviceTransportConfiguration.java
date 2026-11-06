// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.device.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusEndpoint;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpEndpoint;

/**
 * Where one Modbus device is: endpoint plus unit id, with optional per-device overrides of the profile timing.
 * What to read and how to decode it lives in the device profile.
 */
@Schema
@Data
public class ModbusDeviceTransportConfiguration implements DeviceTransportConfiguration {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ModbusEndpoint endpoint = new ModbusTcpEndpoint();
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Unit id (slave address)", example = "1")
    private Integer unitId = 1;
    @Schema(description = "Overrides the profile request timeout, e.g. for a slow serial bus behind a bridge", example = "5000")
    private Integer timeoutMs;
    @Schema(description = "Overrides the profile polling period", example = "10000")
    private Long pollPeriodMs;

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.MODBUS;
    }

    @Override
    public void validate() {
        if (endpoint == null) {
            throw new IllegalArgumentException("Modbus endpoint is required");
        }
        endpoint.validate();
        if (unitId == null || unitId < endpoint.getMinUnitId() || unitId > endpoint.getMaxUnitId()) {
            throw new IllegalArgumentException("Modbus unit id must be in range " + endpoint.getMinUnitId() + ".." + endpoint.getMaxUnitId() + " for this endpoint");
        }
        if (timeoutMs != null && timeoutMs <= 0) {
            throw new IllegalArgumentException("Modbus timeoutMs override must be positive");
        }
        if (pollPeriodMs != null && pollPeriodMs <= 0) {
            throw new IllegalArgumentException("Modbus pollPeriodMs override must be positive");
        }
    }

}
