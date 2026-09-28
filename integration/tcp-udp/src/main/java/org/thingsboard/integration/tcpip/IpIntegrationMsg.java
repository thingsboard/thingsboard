// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tcpip;

import lombok.Data;

import java.net.SocketAddress;

@Data
public class IpIntegrationMsg {

    private final SocketAddress address;
    private final byte[] payload;
}
