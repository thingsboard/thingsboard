// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue.processing;

import lombok.Getter;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;

public class IdMsgPair<T extends com.google.protobuf.GeneratedMessageV3> {
    @Getter
    final UUID uuid;
    @Getter
    final TbProtoQueueMsg<T> msg;

    public IdMsgPair(UUID uuid, TbProtoQueueMsg<T> msg) {
        this.uuid = uuid;
        this.msg = msg;
    }
}
