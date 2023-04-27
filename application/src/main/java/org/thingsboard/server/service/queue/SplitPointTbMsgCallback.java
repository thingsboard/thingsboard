/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.queue;

import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@RequiredArgsConstructor
class SplitPointTbMsgCallback implements TbMsgCallback {

    final AtomicBoolean runOnce = new AtomicBoolean();
    final RuleChainSplitPoint splitPoint;
    final Consumer<TbQueueCallback> failureConsumer;

    @Override
    public void onSuccess() {
        if (runOnce.compareAndSet(false, true)) {
            splitPoint.onSuccess();
        }
    }

    @Override
    public void onFailure(RuleEngineException e) {
        if (runOnce.compareAndSet(false, true)) {
            TbQueueCallback tbQueueCallback = new SplitPointTbQueueCallback();
            failureConsumer.accept(tbQueueCallback);
        }
    }

    private class SplitPointTbQueueCallback implements TbQueueCallback {
        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            splitPoint.onSuccess();
        }

        @Override
        public void onFailure(Throwable t) {
            splitPoint.onFailure(new RuleEngineException(t.getMessage()));
        }
    }
}
