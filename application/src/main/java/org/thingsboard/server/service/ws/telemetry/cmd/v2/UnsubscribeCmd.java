// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import org.thingsboard.server.service.ws.WsCmd;

public interface UnsubscribeCmd extends WsCmd {

    int getCmdId();

}
