/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.actors.calculatedField;

import org.thingsboard.server.common.msg.queue.TbCallback;

import java.util.concurrent.atomic.AtomicInteger;

public class MultipleTbCallback implements TbCallback {

    private final AtomicInteger counter;
    private final TbCallback callback;

    public MultipleTbCallback(int count, TbCallback callback) {
        this.counter = new AtomicInteger(count);
        this.callback = callback;
    }

    @Override
    public void onSuccess() {
        if (counter.decrementAndGet() <= 0) {
            callback.onSuccess();
        }
    }

    public void onSuccess(int number) {
        if (counter.addAndGet(-number) <= 0) {
            callback.onSuccess();
        }
    }

    @Override
    public void onFailure(Throwable t) {
        callback.onFailure(t);
    }
}
