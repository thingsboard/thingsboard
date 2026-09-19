// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data.cmd;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

// mirrors application's org.thingsboard.server.service.ws.WsCommandsWrapper
// NON_NULL: same reasoning as AuthCmd - omit whichever of authCmd/cmds isn't in use
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CmdsWrapper {

    private AuthCmd authCmd;
    private List<EntityDataCmd> cmds;

}
