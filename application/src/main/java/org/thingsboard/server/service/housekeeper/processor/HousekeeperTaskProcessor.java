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
package org.thingsboard.server.service.housekeeper.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.msg.housekeeper.HousekeeperClient;

import java.util.concurrent.Future;

public abstract class HousekeeperTaskProcessor<T extends HousekeeperTask> {

    @Autowired
    protected HousekeeperClient housekeeperClient;

    public abstract void process(T task) throws Exception;

    public abstract HousekeeperTaskType getTaskType();

    public <V> V wait(Future<V> future) throws Exception {
        try {
            return future.get(); // will be interrupted after taskProcessingTimeout
        } catch (InterruptedException e) {
            future.cancel(true); // interrupting the underlying task
            throw e;
        }
    }

}
