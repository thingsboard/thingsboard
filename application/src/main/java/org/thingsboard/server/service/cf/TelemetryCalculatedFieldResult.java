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
import java.util.Map;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;

@Data
@Builder
public final class TelemetryCalculatedFieldResult implements CalculatedFieldResult {

    private final OutputType type;
    private final AttributeScope scope;
    private final OutputStrategy outputStrategy;
    private final JsonNode result;

    @Override
    public TbMsg toTbMsg(EntityId entityId, List<CalculatedFieldId> cfIds) {
        TbMsgType msgType = switch (type) {
            case ATTRIBUTES -> TbMsgType.POST_ATTRIBUTES_REQUEST;
            case TIME_SERIES -> TbMsgType.POST_TELEMETRY_REQUEST;
        };
        TbMsgMetaData metaData = switch (type) {
            case ATTRIBUTES -> new TbMsgMetaData(Map.of(SCOPE, scope.name()));
            case TIME_SERIES -> TbMsgMetaData.EMPTY;
        };
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
