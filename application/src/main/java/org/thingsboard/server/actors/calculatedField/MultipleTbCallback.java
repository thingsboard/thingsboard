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
