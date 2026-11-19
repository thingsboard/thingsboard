// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.transport.modbus.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

import java.io.Serial;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Schema
@Data
public class ModbusSerialEndpoint implements ModbusEndpoint {

    @Serial
    private static final long serialVersionUID = 5089916647450491896L;

    // COM3, com3, \\.\COM10 -> COM3 / COM10
    private static final Pattern WINDOWS_COM =
            Pattern.compile("^(?:\\\\\\\\\\.\\\\)?(com\\d+)$", Pattern.CASE_INSENSITIVE);

    /** Modbus RTU always uses 8 data bits per character, so this is not a setting. */
    public static final int DATA_BITS = 8;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Serial device name or path", example = "/dev/ttyUSB0")
    private String port;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Line speed in bits per second", example = "9600")
    private Integer baudRate = 9600;
    @Schema(description = "Parity; the Modbus specification default is EVEN", example = "EVEN")
    private ModbusSerialParity parity = ModbusSerialParity.EVEN;
    @Schema(description = "Stop bits; the Modbus specification expects TWO when parity is NONE", example = "ONE")
    private ModbusSerialStopBits stopBits = ModbusSerialStopBits.ONE;

    @Schema(description = "RS-485 direction control; omit for RS-232 or adapters that switch direction automatically")
    private ModbusRs485Settings rs485;

    @Override
    public ModbusEndpointType getType() {
        return ModbusEndpointType.SERIAL;
    }

    /**
     * Only the device identifies the bus. All line settings must be identical for every device on it,
     * so a mismatch is a configuration error, not a reason for a second connection (which would fail anyway:
     * the port can only be opened once).
     */
    @Override
    public String getKey() {
        String normalized = port.trim();
        Matcher m = WINDOWS_COM.matcher(normalized);
        if (m.matches()) {
            normalized = m.group(1).toUpperCase(Locale.ROOT); // Windows names are case-insensitive
        }                                                   // Unix paths are not, so leave them alone
        return "serial://" + normalized;
    }

    @Override
    public String getConnectionSettings() {
        return "baud=" + baudRate + ", parity=" + parity + ", stop=" + stopBits
                + ", rs485=" + (rs485 == null ? "off" : rs485.describe());
    }

    @Override
    public int getMinUnitId() {
        return 1;   // 0 is broadcast on serial lines
    }

    @Override
    public int getMaxUnitId() {
        return 247; // 248..255 reserved
    }

    @Override
    public void validate() {
        if (StringUtils.isBlank(port)) {
            throw new IllegalArgumentException("Modbus serial port is required");
        }
        if (baudRate == null || baudRate <= 0) {
            throw new IllegalArgumentException("Modbus serial baud rate must be positive");
        }
        if (parity == null) {
            throw new IllegalArgumentException("Modbus serial parity is required");
        }
        if (stopBits == null) {
            throw new IllegalArgumentException("Modbus serial stop bits are required");
        }
        if (rs485 != null) {
            rs485.validate();
        }
    }
}