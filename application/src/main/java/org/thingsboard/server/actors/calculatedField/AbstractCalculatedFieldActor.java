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
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;

@Slf4j
public abstract class AbstractCalculatedFieldActor extends ContextAwareActor {

    protected final TenantId tenantId;

    public AbstractCalculatedFieldActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
        this.tenantId = tenantId;
    }

    @Override
    protected boolean doProcess(TbActorMsg msg) {
        if (msg instanceof ToCalculatedFieldSystemMsg cfm) {
            Exception cause;
            try {
                return doProcessCfMsg(cfm);
            } catch (CalculatedFieldException cfe) {
                if (DebugModeUtil.isDebugFailuresAvailable(cfe.getCtx().getCalculatedField())) {
                    systemContext.persistCalculatedFieldDebugError(cfe);
                }
                cause = cfe.getCause();
            } catch (Exception e) {
                logProcessingException(e);
                cause = e;
            }
            cfm.getCallback().onFailure(cause);
            return true;
        } else {
            return false;
        }
    }

    abstract void logProcessingException(Exception e);

    abstract boolean doProcessCfMsg(ToCalculatedFieldSystemMsg msg) throws CalculatedFieldException;

}
