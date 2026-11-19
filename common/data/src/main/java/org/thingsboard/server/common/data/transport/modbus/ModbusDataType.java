// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.transport.modbus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ModbusDataType {

    BITS(0),
    STRING(0),
    BYTES(0),
    INT16(1),
    UINT16(1),
    FLOAT16(1),
    INT32(2),
    UINT32(2),
    FLOAT32(2),
    INT64(4),
    UINT64(4),
    FLOAT64(4);

    /** Number of 16-bit registers a value occupies; {@code 0} for variable-length types. */
    private final int registers;

    public boolean isNumeric() {
        return registers > 0;
    }

}
