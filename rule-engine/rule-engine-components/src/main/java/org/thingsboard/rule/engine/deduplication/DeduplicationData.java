// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.deduplication;

import lombok.Data;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.LinkedList;
import java.util.List;

@Data
public class DeduplicationData {

    private final List<TbMsg> msgList;
    private boolean tickScheduled;

    public DeduplicationData() {
        msgList = new LinkedList<>();
    }

    public int size() {
        return msgList.size();
    }

    public void add(TbMsg msg) {
        msgList.add(msg);
    }

    public boolean isEmpty() {
        return msgList.isEmpty();
    }
}
