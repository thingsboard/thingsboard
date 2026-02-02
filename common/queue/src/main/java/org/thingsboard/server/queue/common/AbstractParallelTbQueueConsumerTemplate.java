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
package org.thingsboard.server.queue.common;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractParallelTbQueueConsumerTemplate<R, T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<R, T> {

    protected ListeningExecutorService consumerExecutor;

    public AbstractParallelTbQueueConsumerTemplate(String topic) {
        super(topic);
    }

    protected void initNewExecutor(int threadPoolSize) {
        if (consumerExecutor != null) {
            consumerExecutor.shutdown();
            try {
                consumerExecutor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.trace("Interrupted while waiting for consumer executor to stop");
            }
        }
        consumerExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(threadPoolSize, ThingsBoardThreadFactory.forName(getClass().getSimpleName())));
    }

    protected void shutdownExecutor() {
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
    }

}
