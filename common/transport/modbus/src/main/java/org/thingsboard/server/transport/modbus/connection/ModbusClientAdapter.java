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

}
