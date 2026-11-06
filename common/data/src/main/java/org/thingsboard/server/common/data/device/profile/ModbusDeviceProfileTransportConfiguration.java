// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.transport.modbus.ModbusByteOrder;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;
import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Describes a Modbus device type: what to poll, how to decode it and what can be written.
 * Where the device is (host, port, unit id) lives in the device transport configuration.
 */
@Schema
@Data
public class ModbusDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Request timeout in milliseconds", example = "3000")
    private Integer timeoutMs;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Retries per request after a timeout", example = "1")
    private Integer retries;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Polling period for timeseries in milliseconds; also used for attributes unless attributesPollPeriodMs is set",
            example = "5000")
    private Long pollPeriodMs;
    @Schema(description = "Polling period for attributes in milliseconds; defaults to pollPeriodMs", example = "3600000")
    private Long attributesPollPeriodMs;
    @Schema(description = "Byte order inside one register", example = "BIG")
    private ModbusByteOrder byteOrder = ModbusByteOrder.BIG;
    @Schema(description = "Register order inside 32/64-bit values", example = "BIG")
    private ModbusByteOrder wordOrder = ModbusByteOrder.BIG;
    @Schema(description = "Send polled values only when they change")
    private boolean sendDataOnlyOnChange;

    @ArraySchema(arraySchema = @Schema(description = "Polled values posted as telemetry"))
    private List<ModbusMapping> timeseries;
    @ArraySchema(arraySchema = @Schema(description = "Polled values posted as client attributes"))
    private List<ModbusMapping> attributes;
    @ArraySchema(arraySchema = @Schema(description = "Shared attributes written to the device when they change"))
    private List<ModbusMapping> attributeUpdates;
    @ArraySchema(arraySchema = @Schema(description = "RPC methods, matched by tag; read or write"))
    private List<ModbusMapping> rpc;

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.MODBUS;
    }

    /**
     * Profile-level attribute polling period. The transport resolves the final period per device:
     * attributesPollPeriodMs if set, otherwise the device's pollPeriodMs override, otherwise pollPeriodMs.
     */
    @JsonIgnore
    public Long getEffectiveAttributesPollPeriodMs() {
        return attributesPollPeriodMs != null ? attributesPollPeriodMs : pollPeriodMs;
    }

    @Override
    public void validate() {
        require(timeoutMs != null && timeoutMs > 0, "timeoutMs must be positive");
        require(retries != null && retries >= 0, "retries must not be negative");
        require(pollPeriodMs != null && pollPeriodMs > 0, "pollPeriodMs must be positive");
        require(attributesPollPeriodMs == null || attributesPollPeriodMs > 0, "attributesPollPeriodMs must be positive");
        require(byteOrder != null, "byteOrder is required");
        require(wordOrder != null, "wordOrder is required");
        validateMappings("timeseries", timeseries, fc -> !fc.isWrite());
        validateMappings("attributes", attributes, fc -> !fc.isWrite());
        validateMappings("attributeUpdates", attributeUpdates, ModbusFunctionCode::isWrite);
        validateMappings("rpc", rpc, _ -> true);
    }

    private static void validateMappings(String section, List<ModbusMapping> mappings, Predicate<ModbusFunctionCode> allowedFunctionCodes) {
        if (mappings == null) {
            return;
        }
        Set<String> tags = new HashSet<>();
        for (int i = 0; i < mappings.size(); i++) {
            String path = section + "[" + i + "]";
            ModbusMapping mapping = mappings.get(i);
            require(mapping != null, "Modbus mapping " + path + " is null");
            mapping.validate(path, allowedFunctionCodes);
            require(tags.add(mapping.getTag()), "Modbus mapping " + path + ": duplicate tag '" + mapping.getTag() + "' in " + section);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

}
