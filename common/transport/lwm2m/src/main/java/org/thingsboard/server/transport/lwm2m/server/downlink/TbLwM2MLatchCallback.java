/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
