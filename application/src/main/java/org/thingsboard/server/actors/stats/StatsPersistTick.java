// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.stats;

import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;

public enum StatsPersistTick implements TbActorMsg {
    INSTANCE;

    @Override
    public MsgType getMsgType() {
        return MsgType.STATS_PERSIST_TICK_MSG;
    }
}
