/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.actors.shared;

import akka.actor.ActorContext;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.stats.StatsPersistTick;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

@Slf4j
public abstract class ComponentMsgProcessor<T extends EntityId> extends AbstractContextAwareMsgProcessor {

    protected final TenantId tenantId;
    protected final T entityId;
    protected ComponentLifecycleState state;

    protected ComponentMsgProcessor(ActorSystemContext systemContext, TenantId tenantId, T id) {
        super(systemContext);
        this.tenantId = tenantId;
        this.entityId = id;
    }

    public abstract String getComponentName();

    public abstract void start(ActorContext context) throws Exception;

    public abstract void stop(ActorContext context) throws Exception;

    public abstract void onPartitionChangeMsg(PartitionChangeMsg msg) throws Exception;

    public void onCreated(ActorContext context) throws Exception {
        start(context);
    }

    public void onUpdate(ActorContext context) throws Exception {
        restart(context);
    }

    public void onActivate(ActorContext context) throws Exception {
        restart(context);
    }

    public void onSuspend(ActorContext context) throws Exception {
        stop(context);
    }

    public void onStop(ActorContext context) throws Exception {
        stop(context);
    }

    private void restart(ActorContext context) throws Exception {
        stop(context);
        start(context);
    }

    public void scheduleStatsPersistTick(ActorContext context, long statsPersistFrequency) {
        schedulePeriodicMsgWithDelay(context, new StatsPersistTick(), statsPersistFrequency, statsPersistFrequency);
    }

    protected void checkActive() {
        if (state != ComponentLifecycleState.ACTIVE) {
            log.debug("Component is not active. Current state [{}] for processor [{}][{}] tenant [{}]", state, entityId.getEntityType(), entityId, tenantId);
            throw new IllegalStateException("Rule chain is not active! " + entityId + " - " + tenantId);
        }
    }

}
