// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.calculatedField;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TbCallback;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MultipleTbCallback implements TbCallback {
    @Getter
    private final UUID id;
    private final AtomicInteger counter;
    private final TbCallback callback;

    public MultipleTbCallback(int count, TbCallback callback) {
        id = UUID.randomUUID();
        this.counter = new AtomicInteger(count);
        this.callback = callback;
    }

    @Override
    public void onSuccess() {
        onSuccess(1);
    }

    public void onSuccess(int number) {
        log.trace("[{}][{}] onSuccess({})", id, callback.getId(), number);
        if (counter.addAndGet(-number) <= 0) {
            log.trace("[{}][{}] Done.", id, callback.getId());
            callback.onSuccess();
        }
    }

    @Override
    public void onFailure(Throwable t) {
        log.warn("[{}][{}] onFailure.", id, callback.getId());
        callback.onFailure(t);
    }
}
