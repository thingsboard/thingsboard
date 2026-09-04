// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.common;

import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

public class TbQueueTbMsgCallbackWrapper implements TbQueueCallback {

    private final TbMsgCallback tbMsgCallback;

    public TbQueueTbMsgCallbackWrapper(TbMsgCallback tbMsgCallback) {
        this.tbMsgCallback = tbMsgCallback;
    }

    @Override
    public void onSuccess(TbQueueMsgMetadata metadata) {
        tbMsgCallback.onSuccess();
    }

    @Override
    public void onFailure(Throwable t) {
        tbMsgCallback.onFailure(new RuleEngineException(t.getMessage(), t));
    }
}
