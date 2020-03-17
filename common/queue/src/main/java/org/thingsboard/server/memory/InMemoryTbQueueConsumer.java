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

import org.thingsboard.server.TbQueueConsumer;
import org.thingsboard.server.TbQueueMsg;

import java.util.List;

public class InMemoryTbQueueConsumer<T extends TbQueueMsg> implements TbQueueConsumer<T> {
    private final InMemoryStorage storage = InMemoryStorage.getInstance();

    public InMemoryTbQueueConsumer(String topic) {
        this.topic = topic;
    }

    private final String topic;

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void subscribe(List<Integer> partitions) {

    }

    @Override
    public void unsubscribe() {

    }

    @Override
    public List<T> poll(long durationInMillis) {
        return storage.get(topic, durationInMillis);
    }

    @Override
    public void commit() {
        storage.commit(topic);
    }
}
