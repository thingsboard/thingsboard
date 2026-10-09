// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.common.msg.queue.TbCallback;

class IntegrationApiCallback implements IntegrationCallback<Void> {

    private final TbCallback callback;

    public IntegrationApiCallback(TbCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onSuccess(Void msg) {
        callback.onSuccess();
    }

    @Override
    public void onError(Throwable e) {
        callback.onFailure(e);
    }
}
