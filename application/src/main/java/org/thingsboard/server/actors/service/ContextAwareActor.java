/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.actors.service;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.msg.TbActorMsg;

public abstract class ContextAwareActor extends UntypedActor {
    protected final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    public static final int ENTITY_PACK_LIMIT = 1024;

    protected final ActorSystemContext systemContext;

    public ContextAwareActor(ActorSystemContext systemContext) {
        super();
        this.systemContext = systemContext;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Processing msg: {}", msg);
        }
        if (msg instanceof TbActorMsg) {
            try {
                if (!process((TbActorMsg) msg)) {
                    logger.warning("Unknown message: {}!", msg);
                }
            } catch (Exception e) {
                throw e;
            }
        } else {
            logger.warning("Unknown message: {}!", msg);
        }
    }

    protected abstract boolean process(TbActorMsg msg);
}
