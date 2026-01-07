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
package org.thingsboard.server.actors.calculatedField;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorStopReason;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldPartitionChangeMsg;

@Slf4j
public class CalculatedFieldEntityActor extends AbstractCalculatedFieldActor {

    private final CalculatedFieldEntityMessageProcessor processor;

    CalculatedFieldEntityActor(ActorSystemContext systemContext, TenantId tenantId, EntityId entityId) {
        super(systemContext, tenantId);
        this.processor = new CalculatedFieldEntityMessageProcessor(systemContext, tenantId, entityId);
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        log.debug("[{}][{}] Starting CF entity actor.", processor.tenantId, processor.entityId);
        try {
            processor.init(ctx);
            log.debug("[{}][{}] CF entity actor started.", processor.tenantId, processor.entityId);
        } catch (Exception e) {
            log.warn("[{}][{}] Unknown failure", processor.tenantId, processor.entityId, e);
            throw new TbActorException("Failed to initialize CF entity actor", e);
        }
    }

    @Override
    public void destroy(TbActorStopReason stopReason, Throwable cause) throws TbActorException {
        log.debug("[{}] Stopping CF entity actor.", processor.tenantId);
        processor.stop();
    }

    @Override
    protected boolean doProcessCfMsg(ToCalculatedFieldSystemMsg msg) throws CalculatedFieldException {
        switch (msg.getMsgType()) {
            case CF_PARTITIONS_CHANGE_MSG:
                processor.process((CalculatedFieldPartitionChangeMsg) msg);
                break;
            case CF_STATE_RESTORE_MSG:
                processor.process((CalculatedFieldStateRestoreMsg) msg);
                break;
            case CF_ENTITY_INIT_CF_MSG:
                processor.process((EntityInitCalculatedFieldMsg) msg);
                break;
            case CF_ENTITY_DELETE_MSG:
                processor.process((CalculatedFieldEntityDeleteMsg) msg);
                break;
            case CF_ENTITY_TELEMETRY_MSG:
                processor.process((EntityCalculatedFieldTelemetryMsg) msg);
                break;
            case CF_LINKED_TELEMETRY_MSG:
                processor.process((EntityCalculatedFieldLinkedTelemetryMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    void logProcessingException(Exception e) {
        log.warn("[{}][{}] Processing failure", tenantId, processor.entityId, e);
    }
}
