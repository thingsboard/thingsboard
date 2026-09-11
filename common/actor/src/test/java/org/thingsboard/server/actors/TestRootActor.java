// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

@Slf4j
public class TestRootActor extends AbstractTbActor {

    @Getter
    private final TbActorId actorId;
    @Getter
    private final ActorTestCtx testCtx;

    private boolean initialized;
    private long sum;
    private int count;

    public TestRootActor(TbActorId actorId, ActorTestCtx testCtx) {
        this.actorId = actorId;
        this.testCtx = testCtx;
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        initialized = true;
    }

    @Override
    public boolean process(TbActorMsg msg) {
        if (initialized) {
            int value = ((IntTbActorMsg) msg).getValue();
            sum += value;
            count += 1;
            if (count == testCtx.getExpectedInvocationCount()) {
                testCtx.getActual().set(sum);
                testCtx.getInvocationCount().addAndGet(count);
                sum = 0;
                count = 0;
                testCtx.getLatch().countDown();
            }
        }
        return true;
    }

    @Override
    public void destroy(TbActorStopReason stopReason, Throwable cause) {

    }

    public static class TestRootActorCreator implements TbActorCreator {

        private final TbActorId actorId;
        private final ActorTestCtx testCtx;

        public TestRootActorCreator(TbActorId actorId, ActorTestCtx testCtx) {
            this.actorId = actorId;
            this.testCtx = testCtx;
        }

        @Override
        public TbActorId createActorId() {
            return actorId;
        }

        @Override
        public TbActor createActor() {
            return new TestRootActor(actorId, testCtx);
        }
    }
}
