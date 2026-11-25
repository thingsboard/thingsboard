// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;

import java.util.concurrent.CompletionStage;

/**
 * The subset of a Modbus client the endpoint needs. Keeps {@link ModbusEndpoint} independent of the library and testable.
 */
public interface ModbusClientAdapter {

    CompletionStage<Void> connect();

    CompletionStage<Void> disconnect();

    /**
     * Reads {@code count} objects starting at {@code address} with a read function code (1-4).
     *
     * @return register bytes (2 per register, big-endian as on the wire) or coil bytes (LSB first)
     */
    CompletionStage<byte[]> read(int unitId, ModbusFunctionCode functionCode, int address, int count);

    /** Writes one coil with function code 5, or with function code 15 as a group of one for devices that only support that. */
    CompletionStage<Void> writeCoil(int unitId, ModbusFunctionCode functionCode, int address, boolean value);

    /** Writes registers with function code 6 (one register) or 16 (several); {@code registerBytes} holds 2 bytes per register. */
    CompletionStage<Void> writeRegisters(int unitId, ModbusFunctionCode functionCode, int address, byte[] registerBytes);

    /** Function code 22: the device sets the register to {@code (current AND andMask) OR (orMask AND NOT andMask)}. */
    CompletionStage<Void> maskWriteRegister(int unitId, int address, int andMask, int orMask);

}
