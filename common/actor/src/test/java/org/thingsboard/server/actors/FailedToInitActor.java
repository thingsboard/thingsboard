// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FailedToInitActor extends TestRootActor {

    int retryAttempts;
    int retryDelay;
    int attempts = 0;

    public FailedToInitActor(TbActorId actorId, ActorTestCtx testCtx, int retryAttempts, int retryDelay) {
        super(actorId, testCtx);
        this.retryAttempts = retryAttempts;
        this.retryDelay = retryDelay;
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        if (attempts < retryAttempts) {
            attempts++;
            throw new TbActorException("Test attempt", new RuntimeException());
        } else {
            super.init(ctx);
        }
    }

    @Override
    public InitFailureStrategy onInitFailure(int attempt, Throwable t) {
        return InitFailureStrategy.retryWithDelay(retryDelay);
    }

    public static class FailedToInitActorCreator implements TbActorCreator {

        private final TbActorId actorId;
        private final ActorTestCtx testCtx;
        private final int retryAttempts;
        private final int retryDelay;

        public FailedToInitActorCreator(TbActorId actorId, ActorTestCtx testCtx, int retryAttempts, int retryDelay) {
            this.actorId = actorId;
            this.testCtx = testCtx;
            this.retryAttempts = retryAttempts;
            this.retryDelay = retryDelay;
        }

        @Override
        public TbActorId createActorId() {
            return actorId;
        }

        @Override
        public TbActor createActor() {
            return new FailedToInitActor(actorId, testCtx, retryAttempts, retryDelay);
        }
    }
}
