/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldLinkInitMsg;

/**
 * Created by ashvayka on 15.03.18.
 */
@Slf4j
public class CalculatedFieldManagerActor extends ContextAwareActor {

    private final CalculatedFieldManagerMessageProcessor processor;

    public CalculatedFieldManagerActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
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
    protected boolean doProcess(TbActorMsg msg) {
        switch (msg.getMsgType()) {
            case PARTITION_CHANGE_MSG:
                ctx.broadcastToChildren(msg, true); // TODO
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
            case CF_UPDATE_MSG:
//                processor.onToCalculatedFieldSystemActorMsg((ToCalculatedFieldSystemMsg) msg);
                break;
            case CF_TELEMETRY_MSG:
                processor.onTelemetryMsg((CalculatedFieldTelemetryMsg) msg);
                break;
            case CF_LINKED_TELEMETRY_MSG:
                processor.onLinkedTelemetryMsg((CalculatedFieldLinkedTelemetryMsg) msg);
                break;
            case CF_ENTITY_UPDATE_MSG:
//                processor.onToCalculatedFieldSystemActorMsg((ToCalculatedFieldSystemMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }
}
