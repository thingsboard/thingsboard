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
package org.thingsboard.server.common.data.transport.snmp;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SnmpCommunicationSpec {

    TELEMETRY_QUERYING("telemetryQuerying"),
    CLIENT_ATTRIBUTES_QUERYING("clientAttributesQuerying"),

    SHARED_ATTRIBUTES_SETTING("sharedAttributesSetting"),
    TO_DEVICE_RPC_REQUEST("toDeviceRpcRequest"),

    TO_SERVER_RPC_REQUEST("toServerRpcRequest");

    private final String label;

}
