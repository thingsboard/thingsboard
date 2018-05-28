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
package org.thingsboard.server.actors.stats;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;

public class StatsActor extends ContextAwareActor {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private final ObjectMapper mapper = new ObjectMapper();

    public StatsActor(ActorSystemContext context) {
        super(context);
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        //TODO Move everything here, to work with TbActorMsg\
        return false;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        logger.debug("Received message: {}", msg);
        if (msg instanceof StatsPersistMsg) {
            try {
                onStatsPersistMsg((StatsPersistMsg) msg);
            } catch (Exception e) {
                logger.warning("Failed to persist statistics: {}", msg, e);
            }
        }
    }

    public void onStatsPersistMsg(StatsPersistMsg msg) throws Exception {
        Event event = new Event();
        event.setEntityId(msg.getEntityId());
        event.setTenantId(msg.getTenantId());
        event.setType(DataConstants.STATS);
        event.setBody(toBodyJson(systemContext.getDiscoveryService().getCurrentServer().getServerAddress(), msg.getMessagesProcessed(), msg.getErrorsOccurred()));
        systemContext.getEventService().save(event);
    }

    private JsonNode toBodyJson(ServerAddress server, long messagesProcessed, long errorsOccurred) {
        return mapper.createObjectNode().put("server", server.toString()).put("messagesProcessed", messagesProcessed).put("errorsOccurred", errorsOccurred);
    }

    public static class ActorCreator extends ContextBasedCreator<StatsActor> {
        private static final long serialVersionUID = 1L;

        public ActorCreator(ActorSystemContext context) {
            super(context);
        }

        @Override
        public StatsActor create() throws Exception {
            return new StatsActor(context);
        }
    }
}
