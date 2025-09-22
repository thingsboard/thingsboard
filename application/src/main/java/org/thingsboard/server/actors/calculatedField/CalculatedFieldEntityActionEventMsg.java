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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos.EntityActionEventProto;

@Data
@Builder
public class CalculatedFieldEntityActionEventMsg implements ToCalculatedFieldSystemMsg {

    private final TenantId tenantId;
    private final EntityId entityId;
    private final JsonNode entity;
    private final ActionType action;
    private final TbCallback callback;

    public static CalculatedFieldEntityActionEventMsg fromProto(EntityActionEventProto proto,
                                                                TbCallback callback) {
        return CalculatedFieldEntityActionEventMsg.builder()
                .tenantId((TenantId) ProtoUtils.fromProto(proto.getTenantId()))
                .entityId(ProtoUtils.fromProto(proto.getEntityId()))
                .entity(JacksonUtil.toJsonNode(proto.getEntity()))
                .action(ActionType.valueOf(proto.getAction()))
                .callback(callback)
                .build();
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.CF_ENTITY_ACTION_EVENT_MSG;
    }

}
