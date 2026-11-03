// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.util.sparkplug;

public enum SparkplugConnectionState {
    /**
     * The EoN node should examine the payload of this
     * message to ensure that it is a value of “ONLINE”
     */
    OFFLINE,
    /**
     * If the value is “OFFLINE”, this indicates the Primary Application
     * has lost its MQTT Session to this particular MQTT Server.
     */
    ONLINE

}
