// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.app;

import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;

public class AppInitMsg implements TbActorMsg {

    @Override
    public MsgType getMsgType() {
        return MsgType.APP_INIT_MSG;
    }
}
