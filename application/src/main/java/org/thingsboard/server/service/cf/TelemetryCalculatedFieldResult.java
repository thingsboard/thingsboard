// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.configuration.OutputStrategy;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;

import static org.thingsboard.server.common.data.DataConstants.CF_NAME_METADATA_KEY;
import static org.thingsboard.server.common.data.DataConstants.SCOPE;

@Data
@Builder
public final class TelemetryCalculatedFieldResult implements CalculatedFieldResult {

    private final OutputType type;
    private final AttributeScope scope;
    private final OutputStrategy outputStrategy;
    private final JsonNode result;

    public static final TelemetryCalculatedFieldResult EMPTY = TelemetryCalculatedFieldResult.builder().result(null).build();

    @Override
    public TbMsg toTbMsg(EntityId entityId, String cfName, List<CalculatedFieldId> cfIds) {
        TbMsgType msgType = switch (type) {
            case ATTRIBUTES -> TbMsgType.POST_ATTRIBUTES_REQUEST;
            case TIME_SERIES -> TbMsgType.POST_TELEMETRY_REQUEST;
        };
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue(CF_NAME_METADATA_KEY, cfName);
        if (OutputType.ATTRIBUTES == type) {
            metaData.putValue(SCOPE, scope.name());
        }
        return TbMsg.newMsg()
                .type(msgType)
                .originator(entityId)
                .previousCalculatedFieldIds(cfIds)
                .data(stringValue())
                .metaData(metaData)
                .build();
    }

    @Override
    public String stringValue() {
        return result == null ? null : result.toString();
    }

    @Override
    public boolean isEmpty() {
        return result == null || result.isMissingNode() || result.isNull() ||
                (result.isObject() && result.isEmpty()) ||
                (result.isArray() && result.isEmpty()) ||
                (result.isTextual() && result.asText().isEmpty());
    }

}
