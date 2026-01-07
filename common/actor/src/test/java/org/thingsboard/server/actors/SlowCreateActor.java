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
package org.thingsboard.server.actors;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SlowCreateActor extends TestRootActor {

    public static final int TIMEOUT_AWAIT_MAX_MS = 5000;

    public SlowCreateActor(TbActorId actorId, ActorTestCtx testCtx, CountDownLatch initLatch) {
        super(actorId, testCtx);
        try {
            log.info("awaiting on latch {} ...", initLatch);
            initLatch.await(TIMEOUT_AWAIT_MAX_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("interrupted", e);
        }
        testCtx.getInvocationCount().incrementAndGet();
        log.info("finished {} ...", initLatch);
    }

    public static class SlowCreateActorCreator implements TbActorCreator {

        private final TbActorId actorId;
        private final ActorTestCtx testCtx;
        private final CountDownLatch initLatch;

        public SlowCreateActorCreator(TbActorId actorId, ActorTestCtx testCtx, CountDownLatch initLatch) {
            this.actorId = actorId;
            this.testCtx = testCtx;
            this.initLatch = initLatch;
        }

        @Override
        public TbActorId createActorId() {
            return actorId;
        }

        @Override
        public TbActor createActor() {
            log.info("creating slow actor...");
            SlowCreateActor slowCreateActor = new SlowCreateActor(actorId, testCtx, initLatch);
            log.info("created slow actor {}", slowCreateActor);
            return slowCreateActor;
        }
    }
}
