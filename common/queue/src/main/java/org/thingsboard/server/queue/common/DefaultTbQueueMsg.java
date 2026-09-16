// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.common;

import lombok.Data;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.UUID;

@Data
public class DefaultTbQueueMsg implements TbQueueMsg {
    private final UUID key;
    private final byte[] data;
    private final DefaultTbQueueMsgHeaders headers;

    public DefaultTbQueueMsg(TbQueueMsg msg) {
        this.key = msg.getKey();
        this.data = msg.getData();
        DefaultTbQueueMsgHeaders headers = new DefaultTbQueueMsgHeaders();
        msg.getHeaders().getData().forEach(headers::put);
        this.headers = headers;
    }

}
