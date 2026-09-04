// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.CountDownLatch;

@RequiredArgsConstructor
public class TbLwM2MLatchCallback<R, T> implements DownlinkRequestCallback<R, T> {

    private final CountDownLatch countDownLatch;
    private final DownlinkRequestCallback<R, T> callback;

    @Override
    public void onSuccess(R request, T response) {
        callback.onSuccess(request, response);
        countDownLatch.countDown();
    }

    @Override
    public void onValidationError(String params, String msg) {
        callback.onValidationError(params, msg);
        countDownLatch.countDown();
    }

    @Override
    public void onError(String params, Exception e) {
        callback.onError(params, e);
        countDownLatch.countDown();
    }
}
