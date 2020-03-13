/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.memory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import org.thingsboard.server.TbQueueCallback;
import org.thingsboard.server.TbQueueMsg;
import org.thingsboard.server.TbQueueMsgMetadata;
import org.thingsboard.server.TbQueueProducer;

@Data
public class InMemoryTbQueueProducer<T extends TbQueueMsg> implements TbQueueProducer<T> {

    private final InMemoryStorage storage = InMemoryStorage.getInstance();

    private final String defaultTopic;

    @Override
    public void init() {

    }

    @Override
    public String getDefaultTopic() {
        return defaultTopic;
    }

    @Override
    public ListenableFuture<TbQueueMsgMetadata> send(T msg, TbQueueCallback callback) {
        return send(defaultTopic, msg, callback);
    }

    @Override
    public ListenableFuture<TbQueueMsgMetadata> send(String topic, T msg, TbQueueCallback callback) {
        boolean result = storage.put(topic, msg);
        if (result) {
            callback.onSuccess(null);
            return Futures.immediateCheckedFuture(null);
        } else {
            Exception e = new RuntimeException("Failure add msg to InMemoryQueue");
            callback.onFailure(e);
            return Futures.immediateFailedFuture(e);
        }
    }
}
