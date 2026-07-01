/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
