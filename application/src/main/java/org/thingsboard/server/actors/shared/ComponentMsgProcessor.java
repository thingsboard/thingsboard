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
package org.thingsboard.server.actors.shared;

import akka.actor.ActorContext;
import akka.event.LoggingAdapter;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.stats.StatsPersistTick;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ClusterEventMsg;
import org.thingsboard.server.service.queue.MsgQueueService;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public abstract class ComponentMsgProcessor<T extends EntityId> extends AbstractContextAwareMsgProcessor {

    protected final TenantId tenantId;
    protected final T entityId;
    protected final MsgQueueService queue;
    protected ComponentLifecycleState state;

    protected ComponentMsgProcessor(ActorSystemContext systemContext, LoggingAdapter logger, TenantId tenantId, T id) {
        super(systemContext, logger);
        this.tenantId = tenantId;
        this.entityId = id;
        this.queue = systemContext.getMsgQueueService();
    }

    public abstract void start(ActorContext context) throws Exception;

    public abstract void stop(ActorContext context) throws Exception;

    public abstract void onClusterEventMsg(ClusterEventMsg msg) throws Exception;

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
            throw new IllegalStateException("Rule chain is not active!");
        }
    }

    protected void putToQueue(final TbMsg tbMsg, final Consumer<TbMsg> onSuccess) {
        EntityId entityId = tbMsg.getRuleNodeId() != null ? tbMsg.getRuleNodeId() : tbMsg.getRuleChainId();
        Futures.addCallback(queue.put(this.tenantId, tbMsg, entityId.getId(), tbMsg.getClusterPartition()), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                onSuccess.accept(tbMsg);
            }

            @Override
            public void onFailure(Throwable t) {
                logger.debug("Failed to push message [{}] to queue due to [{}]", tbMsg, t);
            }
        });
    }
}
