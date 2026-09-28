// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.mqtt.util;

import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;

public final class MqttRpcStatusUtil {

    private MqttRpcStatusUtil() {
    }

    /**
     * Whether the transport should track and emit a delivery status (DELIVERED / TIMEOUT) for the given RPC.
     * Non-persistent one-way RPCs self-complete on send in the device actor and are never added to its pending
     * map, so a delivery status for them lands on nothing and only produces a benign
     * "RPC has already been removed from pending map" warning. Every other RPC is tracked.
     */
    public static boolean requireDeliveryTracking(ToDeviceRpcRequestMsg rpc) {
        return !(rpc.getOneway() && !rpc.getPersisted());
    }

}
