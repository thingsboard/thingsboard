// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
