// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.common;

import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.concurrent.atomic.AtomicInteger;

public class MultipleTbQueueCallbackWrapper implements TbQueueCallback {

    private final AtomicInteger tbQueueCallbackCount;
    private final TbQueueCallback callback;

    public MultipleTbQueueCallbackWrapper(int tbQueueCallbackCount, TbQueueCallback callback) {
        this.tbQueueCallbackCount = new AtomicInteger(tbQueueCallbackCount);
        this.callback = callback;
    }

    @Override
    public void onSuccess(TbQueueMsgMetadata metadata) {
        if (tbQueueCallbackCount.decrementAndGet() <= 0) {
            callback.onSuccess(metadata);
        }
    }

    @Override
    public void onFailure(Throwable t) {
        callback.onFailure(new RuleEngineException(t.getMessage(), t));
    }
}
