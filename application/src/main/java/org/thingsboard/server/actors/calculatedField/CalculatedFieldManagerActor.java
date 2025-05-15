/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorStopReason;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldCacheInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldEntityLifecycleMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldInitProfileEntityMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldLinkInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldPartitionChangeMsg;

/**
 * Created by ashvayka on 15.03.18.
 */
@Slf4j
public class CalculatedFieldManagerActor extends AbstractCalculatedFieldActor {

    private final CalculatedFieldManagerMessageProcessor processor;

    public CalculatedFieldManagerActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext, tenantId);
        this.processor = new CalculatedFieldManagerMessageProcessor(systemContext, tenantId);
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        log.debug("[{}] Starting CF manager actor.", processor.tenantId);
        try {
            processor.init(ctx);
            log.debug("[{}] CF manager actor started.", processor.tenantId);
        } catch (Exception e) {
            log.warn("[{}] Unknown failure", processor.tenantId, e);
            throw new TbActorException("Failed to initialize manager actor", e);
        }
    }

    @Override
    public void destroy(TbActorStopReason stopReason, Throwable cause) throws TbActorException {
        log.debug("[{}] Stopping CF manager actor.", processor.tenantId);
        processor.stop();
    }

    @Override
    protected boolean doProcessCfMsg(ToCalculatedFieldSystemMsg msg) throws CalculatedFieldException {
        switch (msg.getMsgType()) {
            case CF_PARTITIONS_CHANGE_MSG:
                processor.onPartitionChange((CalculatedFieldPartitionChangeMsg) msg);
                break;
            case CF_CACHE_INIT_MSG:
                processor.onCacheInitMsg((CalculatedFieldCacheInitMsg) msg);
                break;
            case CF_INIT_PROFILE_ENTITY_MSG:
                processor.onProfileEntityMsg((CalculatedFieldInitProfileEntityMsg) msg);
                break;
            case CF_INIT_MSG:
                processor.onFieldInitMsg((CalculatedFieldInitMsg) msg);
                break;
            case CF_LINK_INIT_MSG:
                processor.onLinkInitMsg((CalculatedFieldLinkInitMsg) msg);
                break;
            case CF_STATE_RESTORE_MSG:
                processor.onStateRestoreMsg((CalculatedFieldStateRestoreMsg) msg);
                break;
            case CF_ENTITY_LIFECYCLE_MSG:
                processor.onEntityLifecycleMsg((CalculatedFieldEntityLifecycleMsg) msg);
                break;
            case CF_TELEMETRY_MSG:
                processor.onTelemetryMsg((CalculatedFieldTelemetryMsg) msg);
                break;
            case CF_LINKED_TELEMETRY_MSG:
                processor.onLinkedTelemetryMsg((CalculatedFieldLinkedTelemetryMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    void logProcessingException(Exception e) {
        log.warn("[{}] Processing failure", tenantId, e);
    }

}
