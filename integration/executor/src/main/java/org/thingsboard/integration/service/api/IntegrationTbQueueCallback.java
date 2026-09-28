// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.concurrent.ExecutorService;

@Slf4j
@RequiredArgsConstructor
public class IntegrationTbQueueCallback implements TbQueueCallback {

    private final ExecutorService callbackExecutor;
    private final IntegrationCallback<Void> callback;

    @Override
    public void onSuccess(TbQueueMsgMetadata metadata) {
        try {
            callbackExecutor.submit(() -> callback.onSuccess(null));
        } catch (Exception e) {
            log.warn("Failed to submit success callback", e);
            try {
                callback.onError(e);
            } catch (Exception inner) {
                log.warn("Failed to invoke failure callback directly", inner);
            }
        }
    }

    @Override
    public void onFailure(Throwable t) {
        try {
            callbackExecutor.submit(() -> callback.onError(t));
        } catch (Exception e) {
            log.warn("Failed to submit failure callback for error: {}", t.getMessage(), e);
            try {
                callback.onError(t);
            } catch (Exception inner) {
                log.warn("Failed to invoke failure callback directly", inner);
            }
        }
    }

}
