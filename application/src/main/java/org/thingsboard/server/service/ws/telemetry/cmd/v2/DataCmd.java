// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import lombok.Data;
import lombok.Getter;
import org.thingsboard.server.service.ws.WsCmd;

@Data
public abstract class DataCmd implements WsCmd {

    @Getter
    private final int cmdId;

    public DataCmd(int cmdId) {
        this.cmdId = cmdId;
    }

}
