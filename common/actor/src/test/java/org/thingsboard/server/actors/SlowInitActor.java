// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SlowInitActor extends TestRootActor {

    public SlowInitActor(TbActorId actorId, ActorTestCtx testCtx) {
        super(actorId, testCtx);
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.init(ctx);
    }

    public static class SlowInitActorCreator implements TbActorCreator {

        private final TbActorId actorId;
        private final ActorTestCtx testCtx;

        public SlowInitActorCreator(TbActorId actorId, ActorTestCtx testCtx) {
            this.actorId = actorId;
            this.testCtx = testCtx;
        }

        @Override
        public TbActorId createActorId() {
            return actorId;
        }

        @Override
        public TbActor createActor() {
            return new SlowInitActor(actorId, testCtx);
        }
    }
}
