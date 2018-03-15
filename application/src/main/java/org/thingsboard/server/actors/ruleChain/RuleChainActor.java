/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.actors.ruleChain;

import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ComponentActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;

public class RuleChainActor extends ComponentActor<RuleChainId, RuleChainActorMessageProcessor> {

    private RuleChainActor(ActorSystemContext systemContext, TenantId tenantId, RuleChainId ruleChainId) {
        super(systemContext, tenantId, ruleChainId);
        setProcessor(new RuleChainActorMessageProcessor(tenantId, ruleChainId, systemContext,
                logger, context().parent(), context().self()));
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        logger.debug("[{}][{}] Unknown msg type.", tenantId, id, msg.getClass().getName());
    }

    public static class ActorCreator extends ContextBasedCreator<RuleChainActor> {
        private static final long serialVersionUID = 1L;

        private final TenantId tenantId;
        private final RuleChainId ruleChainId;

        public ActorCreator(ActorSystemContext context, TenantId tenantId, RuleChainId pluginId) {
            super(context);
            this.tenantId = tenantId;
            this.ruleChainId = pluginId;
        }

        @Override
        public RuleChainActor create() throws Exception {
            return new RuleChainActor(context, tenantId, ruleChainId);
        }
    }

    @Override
    protected long getErrorPersistFrequency() {
        return systemContext.getPluginErrorPersistFrequency();
    }
}
