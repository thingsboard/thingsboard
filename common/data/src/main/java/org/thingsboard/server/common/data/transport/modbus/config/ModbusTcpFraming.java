// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.transport.modbus.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * How Modbus frames are carried over a TCP connection.
 */
@Getter
@RequiredArgsConstructor
public enum ModbusTcpFraming {

    /** Modbus TCP: MBAP header + PDU. Unit id 0..255; a TCP-to-RTU gateway uses it to select the serial slave. */
    MBAP(0, 255),
    /** RTU over TCP: raw RTU frames (address + PDU + CRC16) tunnelled by a serial-to-TCP bridge. 0 is broadcast, so 1..247. */
    RTU(1, 247);

    private final int minUnitId;
    private final int maxUnitId;

}
