// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.queue;

import lombok.Data;
import lombok.Getter;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;

/**
 * @author Andrew Shvayka
 */
@Data
public final class PartitionChangeMsg implements TbActorMsg {

    @Getter
    private final ServiceType serviceType;

    @Override
    public MsgType getMsgType() {
        return MsgType.PARTITION_CHANGE_MSG;
    }
}
