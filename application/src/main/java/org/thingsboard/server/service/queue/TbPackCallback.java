// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TbCallback;

import java.util.UUID;

@Slf4j
public class TbPackCallback<T> implements TbCallback {
    private final TbPackProcessingContext<T> ctx;
    @Getter
    private final UUID id;

    public TbPackCallback(UUID id, TbPackProcessingContext<T> ctx) {
        log.trace("[{}] CALLBACK CREATED", id);
        this.id = id;
        this.ctx = ctx;
    }

    @Override
    public void onSuccess() {
        log.trace("[{}] ON SUCCESS", id);
        ctx.onSuccess(id);
    }

    @Override
    public void onFailure(Throwable t) {
        log.trace("[{}] ON FAILURE", id, t);
        ctx.onFailure(id, t);
    }
}
