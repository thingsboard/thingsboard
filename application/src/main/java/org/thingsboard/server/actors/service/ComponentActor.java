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
package org.thingsboard.server.actors.service;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.actors.TbRuleNodeUpdateException;
import org.thingsboard.server.actors.shared.ComponentMsgProcessor;
import org.thingsboard.server.actors.stats.StatsPersistMsg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbActorStopReason;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public abstract class ComponentActor<T extends EntityId, P extends ComponentMsgProcessor<T>> extends ContextAwareActor {

    private long lastPersistedErrorTs = 0L;
    protected final TenantId tenantId;
    protected final T id;
    protected P processor;
    private long messagesProcessed;
    private long errorsOccurred;
    ScheduledFuture<?> statsScheduledFuture = null;

    public ComponentActor(ActorSystemContext systemContext, TenantId tenantId, T id) {
        super(systemContext);
        this.tenantId = tenantId;
        this.id = id;
    }

    abstract protected P createProcessor(TbActorCtx ctx);

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        this.processor = createProcessor(ctx);
        initProcessor(ctx);
    }

    protected void initProcessor(TbActorCtx ctx) throws TbActorException {
        try {
            log.debug("[{}][{}][{}] Starting processor.", tenantId, id, id.getEntityType());
            processor.start(ctx);
            logLifecycleEvent(ComponentLifecycleEvent.STARTED);
            if (systemContext.isStatisticsEnabled()) {
                scheduleStatsPersistTick();
            }
        } catch (Exception e) {
            log.debug("[{}][{}] Failed to start {} processor.", tenantId, id, id.getEntityType(), e);
            logAndPersist("OnStart", e, true);
            logLifecycleEvent(ComponentLifecycleEvent.STARTED, e);
            throw new TbActorException("Failed to init actor", e);
        }
    }

    void scheduleStatsPersistTick() {
        try {
            this.statsScheduledFuture = processor.scheduleStatsPersistTick(ctx, systemContext.getStatisticsPersistFrequency());
        } catch (Exception e) {
            log.error("[{}][{}] Failed to schedule statistics store message. No statistics is going to be stored: {}", tenantId, id, e.getMessage());
            logAndPersist("onScheduleStatsPersistMsg", e);
        }
    }

    @Override
    public void destroy(TbActorStopReason stopReason, Throwable cause) {
        try {
            log.debug("[{}][{}][{}] Stopping processor.", tenantId, id, id.getEntityType());
            if (processor != null) {
                processor.stop(ctx);
            }
            logLifecycleEvent(ComponentLifecycleEvent.STOPPED);
            Optional.ofNullable(statsScheduledFuture).ifPresent(x -> x.cancel(false));
            statsScheduledFuture = null;
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to stop {} processor: {}", tenantId, id, id.getEntityType(), e.getMessage());
            logAndPersist("OnStop", e, true);
            logLifecycleEvent(ComponentLifecycleEvent.STOPPED, e);
        }
    }

    protected void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        log.debug("[{}][{}][{}] onComponentLifecycleMsg: [{}]", tenantId, id, id.getEntityType(), msg.getEvent());
        try {
            switch (msg.getEvent()) {
                case CREATED:
                    processor.onCreated(ctx);
                    break;
                case UPDATED:
                    processor.onUpdate(ctx);
                    break;
                case ACTIVATED:
                    processor.onActivate(ctx);
                    break;
                case SUSPENDED:
                    processor.onSuspend(ctx);
                    break;
                case DELETED:
                    processor.onStop(ctx);
                    ctx.stop(ctx.getSelf());
                    break;
                default:
                    break;
            }
            logLifecycleEvent(msg.getEvent());
        } catch (Exception e) {
            logAndPersist("onLifecycleMsg", e, true);
            logLifecycleEvent(msg.getEvent(), e);
            if (e instanceof TbRuleNodeUpdateException) {
                throw (TbRuleNodeUpdateException) e;
            }
        }
    }

    protected void onClusterEventMsg(PartitionChangeMsg msg) {
        try {
            processor.onPartitionChangeMsg(msg);
        } catch (Exception e) {
            logAndPersist("onClusterEventMsg", e);
        }
    }

    protected void onStatsPersistTick(EntityId entityId) {
        try {
            systemContext.getStatsActor().tell(new StatsPersistMsg(messagesProcessed, errorsOccurred, tenantId, entityId));
            resetStatsCounters();
        } catch (Exception e) {
            logAndPersist("onStatsPersistTick", e);
        }
    }

    private void resetStatsCounters() {
        messagesProcessed = 0;
        errorsOccurred = 0;
    }

    protected void increaseMessagesProcessedCount() {
        messagesProcessed++;
    }

    protected void logAndPersist(String method, Exception e) {
        logAndPersist(method, e, false);
    }

    private void logAndPersist(String method, Exception e, boolean critical) {
        errorsOccurred++;
        String componentName = processor != null ? processor.getComponentName() : "Unknown";
        if (critical) {
            log.debug("[{}][{}][{}] Failed to process method: {}", id, tenantId, componentName, method);
            log.debug("Critical Error: ", e);
        } else {
            log.trace("[{}][{}][{}] Failed to process method: {}", id, tenantId, componentName, method);
            log.trace("Debug Error: ", e);
        }
        long ts = System.currentTimeMillis();
        if (ts - lastPersistedErrorTs > getErrorPersistFrequency()) {
            systemContext.persistError(tenantId, id, method, e);
            lastPersistedErrorTs = ts;
        }
    }

    private void logLifecycleEvent(ComponentLifecycleEvent event) {
        logLifecycleEvent(event, null);
    }

    protected void logLifecycleEvent(ComponentLifecycleEvent event, Exception e) {
        systemContext.persistLifecycleEvent(tenantId, id, event, e);
    }

    protected abstract long getErrorPersistFrequency();

}
