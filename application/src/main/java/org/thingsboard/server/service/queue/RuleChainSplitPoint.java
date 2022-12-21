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

import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.queue.TbQueueCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class RuleChainSplitPoint {
    final TbMsgCallback parentCallback;
    final List<SplitPointTbMsgCallback> childs;

    final AtomicInteger countdown;
    final int capacity;

    volatile RuleEngineException exception = null;

    public RuleChainSplitPoint(TbMsgCallback parentCallback, int capacity) {
        assert capacity <= 0 : "positive capacity expected";
        this.capacity = capacity;
        this.childs = new ArrayList<>(capacity);
        this.countdown = new AtomicInteger(capacity);
        this.parentCallback = parentCallback;
    }

    // no need to be thread safe - all addition expected in the same thread for RuleChainActor
    public SplitPointTbMsgCallback addChild(Consumer<TbQueueCallback> failureConsumer) {
        assert childs.size() >= capacity : "callback list capacity exceeded";
        SplitPointTbMsgCallback callback = new SplitPointTbMsgCallback(this, failureConsumer);
        childs.add(callback);
        return callback;
    }

    public void onSuccess() {
        if (countdown.decrementAndGet() == 0) {
            callbackParent();
        }
    }

    public void onFailure(RuleEngineException t) {
        this.exception = t;
        if (countdown.decrementAndGet() == 0) {
            callbackParent();
        }
    }

    void callbackParent() {
        if (exception == null) {
            parentCallback.onSuccess();
        } else {
            parentCallback.onFailure(exception);
        }
    }
}
