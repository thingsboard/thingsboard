// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.transport.modbus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Modbus function codes supported by the transport.
 * Serialized as the numeric code (e.g. {@code "functionCode": 3}) to stay compatible with the IoT Gateway Modbus connector config.
 */
@Getter
@RequiredArgsConstructor
public enum ModbusFunctionCode {

    READ_COILS(1, true, false, 2000),
    READ_DISCRETE_INPUTS(2, true, false, 2000),
    READ_HOLDING_REGISTERS(3, false, false, 125),
    READ_INPUT_REGISTERS(4, false, false, 125),
    WRITE_SINGLE_COIL(5, true, true, 1),
    WRITE_SINGLE_REGISTER(6, false, true, 1),
    WRITE_MULTIPLE_COILS(15, true, true, 1968),
    WRITE_MULTIPLE_REGISTERS(16, false, true, 123),
    /** Atomically sets or clears bits of one holding register on the device; optional in the spec, so some devices reply with exception 01. */
    MASK_WRITE_REGISTER(22, false, true, 1);

    @JsonValue
    private final int code;
    /** {@code true} for coils / discrete inputs (1-bit objects), {@code false} for 16-bit registers. */
    private final boolean bitAccess;
    private final boolean write;
    /** Maximum number of objects per request, per the Modbus Application Protocol v1.1b3. */
    private final int maxCount;

    @JsonCreator
    public static ModbusFunctionCode fromCode(int code) {
        for (ModbusFunctionCode functionCode : values()) {
            if (functionCode.code == code) {
                return functionCode;
            }
        }
        throw new IllegalArgumentException("Unsupported Modbus function code: " + code);
    }

}
