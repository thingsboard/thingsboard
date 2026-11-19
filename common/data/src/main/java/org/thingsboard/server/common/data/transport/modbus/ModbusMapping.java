// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.transport.modbus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.StringUtils;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 * One Modbus object range mapped to a ThingsBoard key.
 * Field names follow the IoT Gateway Modbus connector config.
 */
@Schema
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModbusMapping implements Serializable {

    private static final long serialVersionUID = 4271834196540221307L;

    private static final int MAX_ADDRESS = 0xFFFF;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Telemetry/attribute key or RPC method name", example = "voltage")
    private String tag;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Value type", example = "FLOAT32")
    private ModbusDataType type;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "integer", allowableValues = {"1", "2", "3", "4", "5", "6", "15", "16", "22"}, example = "4")
    private ModbusFunctionCode functionCode;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "0-based protocol (PDU) address", example = "0")
    private Integer address;
    @Schema(description = "Number of registers. Required for STRING and BYTES; derived from the type for numeric types (must match if set); always 1 for BITS",
            example = "8")
    private Integer objectsCount;
    @Schema(description = "Bit index (0-15) inside a register, for BITS read from register function codes or written with function code 22")
    private Integer bit;
    @Schema(description = "Optional scale applied to numeric values (raw * multiplier)", example = "0.1")
    private Double multiplier;

    /**
     * Number of objects this mapping spans: {@code objectsCount} when set, otherwise derived from the type.
     * {@code null} only for STRING/BYTES without {@code objectsCount}, which {@link #validate} rejects.
     */
    @JsonIgnore
    public Integer getEffectiveObjectsCount() {
        if (objectsCount != null) {
            return objectsCount;
        }
        if (type == null) {
            return null;
        }
        if (type == ModbusDataType.BITS) {
            return 1;
        }
        return type.isNumeric() ? type.getRegisters() : null;
    }

    public void validate(String path, Predicate<ModbusFunctionCode> allowedFunctionCodes) {
        require(StringUtils.isNotBlank(tag), path, "tag is required");
        require(type != null, path, "type is required");
        require(functionCode != null, path, "functionCode is required");
        require(allowedFunctionCodes.test(functionCode), path, "function code " + functionCode.getCode() + " is not allowed here");
        require(address != null && address >= 0 && address <= MAX_ADDRESS, path, "address must be in range 0.." + MAX_ADDRESS);

        if (type == ModbusDataType.BITS) {
            require(objectsCount == null || objectsCount == 1, path, "objectsCount must be 1 for BITS");
        } else if (type.isNumeric()) {
            require(objectsCount == null || objectsCount == type.getRegisters(), path, type + " requires objectsCount " + type.getRegisters());
            require(type.getRegisters() <= functionCode.getMaxCount(), path,
                    type + " spans " + type.getRegisters() + " registers; function code " + functionCode.getCode()
                            + " handles only " + functionCode.getMaxCount() + ", use function code 16");
        } else {
            require(objectsCount != null, path, "objectsCount is required for " + type);
        }
        int count = getEffectiveObjectsCount();
        require(count >= 1 && count <= functionCode.getMaxCount(), path,
                "objectsCount must be in range 1.." + functionCode.getMaxCount() + " for function code " + functionCode.getCode());
        require(address + count - 1 <= MAX_ADDRESS, path, "address range exceeds " + MAX_ADDRESS);

        if (functionCode.isBitAccess()) {
            require(type == ModbusDataType.BITS, path, "coil and discrete input function codes support only BITS");
            require(bit == null, path, "bit is only applicable to register function codes");
        } else if (type == ModbusDataType.BITS) {
            require(bit != null && bit >= 0 && bit <= 15, path, "bit must be in range 0..15");
            require(!functionCode.isWrite() || functionCode == ModbusFunctionCode.MASK_WRITE_REGISTER, path,
                    "writing a single bit of a register requires function code 22 (mask write); function codes 6 and 16 overwrite the whole register");
        } else {
            require(functionCode != ModbusFunctionCode.MASK_WRITE_REGISTER, path, "function code 22 writes a single bit; use type BITS with bit");
            require(bit == null, path, "bit is only applicable to BITS");
        }

        if (multiplier != null) {
            require(type.isNumeric(), path, "multiplier is only applicable to numeric types");
            require(Double.isFinite(multiplier) && multiplier != 0, path, "multiplier must be a finite non-zero number");
        }
    }

    private static void require(boolean condition, String path, String message) {
        if (!condition) {
            throw new IllegalArgumentException("Modbus mapping " + path + ": " + message);
        }
    }

}
