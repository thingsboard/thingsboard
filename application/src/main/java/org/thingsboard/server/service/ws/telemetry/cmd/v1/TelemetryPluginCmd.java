// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v1;

import org.thingsboard.server.service.ws.WsCmd;

/**
 * @author Andrew Shvayka
 */
public interface TelemetryPluginCmd extends WsCmd {

    int getCmdId();

    void setCmdId(int cmdId);

    String getKeys();

}
