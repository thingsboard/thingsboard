// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
