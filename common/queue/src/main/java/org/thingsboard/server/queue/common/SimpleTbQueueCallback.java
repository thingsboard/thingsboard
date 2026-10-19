// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.common;

import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.function.Consumer;

public class SimpleTbQueueCallback implements TbQueueCallback {

    private final Consumer<TbQueueMsgMetadata> onSuccess;
    private final Consumer<Throwable> onFailure;

    public SimpleTbQueueCallback(Consumer<TbQueueMsgMetadata> onSuccess, Consumer<Throwable> onFailure) {
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    @Override
    public void onSuccess(TbQueueMsgMetadata metadata) {
        if (onSuccess != null) {
            onSuccess.accept(metadata);
        }
    }

    @Override
    public void onFailure(Throwable t) {
        if (onFailure != null) {
            onFailure.accept(t);
        }
    }

}
