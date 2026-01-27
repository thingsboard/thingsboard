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

import lombok.Data;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

@Data
public class CalculatedFieldRelationActionMsg implements ToCalculatedFieldSystemMsg {

    private final TenantId tenantId;
    private final EntityId relatedEntityId;
    private final ActionType action;
    private final CalculatedFieldCtx calculatedField;
    private final TbCallback callback;

    public CalculatedFieldRelationActionMsg(TenantId tenantId,
                                            EntityId relatedEntityId, ActionType action,
                                            CalculatedFieldCtx calculatedField,
                                            TbCallback callback) {
        this.tenantId = tenantId;
        this.relatedEntityId = relatedEntityId;
        this.action = action;
        this.calculatedField = calculatedField;
        this.callback = callback;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.CF_RELATION_ACTION_MSG;
    }

}
