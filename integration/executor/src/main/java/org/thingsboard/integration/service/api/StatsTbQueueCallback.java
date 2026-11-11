// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service.api;

import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

@RequiredArgsConstructor
public class StatsTbQueueCallback implements TbQueueCallback {

    private final TbQueueCallback callback;
    private final MessagesStats stats;

    @Override
    public void onSuccess(TbQueueMsgMetadata metadata) {
        stats.incrementSuccessful();
        if (callback != null) {
            callback.onSuccess(metadata);
        }
    }

    @Override
    public void onFailure(Throwable t) {
        stats.incrementFailed();
        if (callback != null) {
            callback.onFailure(t);
        }
    }
}
