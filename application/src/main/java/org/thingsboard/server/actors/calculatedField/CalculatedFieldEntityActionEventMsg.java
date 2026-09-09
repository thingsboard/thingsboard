// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
