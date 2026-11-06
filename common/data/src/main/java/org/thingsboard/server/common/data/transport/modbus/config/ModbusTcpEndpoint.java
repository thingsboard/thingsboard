// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.transport.modbus.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

import java.util.Locale;

@Schema
@Data
public class ModbusTcpEndpoint implements ModbusEndpoint {

    private static final long serialVersionUID = -2378563149305812244L;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Device or serial-to-TCP bridge host", example = "10.0.0.30")
    private String host = "localhost";
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "502")
    private Integer port = 502;
    @Schema(description = "MBAP for Modbus TCP, RTU for RTU frames tunnelled over TCP", example = "MBAP")
    private ModbusTcpFraming framing = ModbusTcpFraming.MBAP;

    @Override
    public ModbusEndpointType getType() {
        return ModbusEndpointType.TCP;
    }

    /**
     * Framing is deliberately not part of the key: two devices behind the same host:port must use one connection,
     * so a framing mismatch between them is a configuration error the transport reports, not a reason for a second connection.
     */
    @Override
    public String getKey() {
        String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
        if (normalizedHost.contains(":") && !normalizedHost.startsWith("[")) {
            normalizedHost = "[" + normalizedHost + "]";
        }
        return "tcp://" + normalizedHost + ":" + port;
    }

    @Override
    public int getMinUnitId() {
        return framing.getMinUnitId();
    }

    @Override
    public int getMaxUnitId() {
        return framing.getMaxUnitId();
    }

    @Override
    public void validate() {
        if (StringUtils.isBlank(host)) {
            throw new IllegalArgumentException("Modbus endpoint host is required");
        }
        if (port == null || port < 1 || port > 65535) {
            throw new IllegalArgumentException("Modbus endpoint port must be in range 1..65535");
        }
        if (framing == null) {
            throw new IllegalArgumentException("Modbus endpoint framing is required");
        }
    }

}
