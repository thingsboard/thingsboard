/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.constructor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.gen.edge.EntityDataProto;

@Component
@Slf4j
public class EntityDataMsgConstructor {

    public EntityDataProto constructEntityDataMsg(EntityId entityId, ActionType actionType, JsonElement entityData) {
        EntityDataProto.Builder builder = EntityDataProto.newBuilder()
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setEntityType(entityId.getEntityType().name());
        switch (actionType) {
            case TIMESERIES_UPDATED:
                try {
                    builder.setPostTelemetryMsg(JsonConverter.convertToTelemetryProto(entityData));
                } catch (Exception e) {
                    log.warn("Can't convert to telemetry proto, entityData [{}]", entityData, e);
                }
                break;
            case ATTRIBUTES_UPDATED:
                try {
                    JsonObject data = entityData.getAsJsonObject();
                    builder.setPostAttributesMsg(JsonConverter.convertToAttributesProto(data.getAsJsonObject("kv")));
                    builder.setPostAttributeScope(data.getAsJsonPrimitive("scope").getAsString());
                } catch (Exception e) {
                    log.warn("Can't convert to attributes proto, entityData [{}]", entityData, e);
                }
                break;
            // TODO: voba - add support for attribute delete
            // case ATTRIBUTES_DELETED:
        }
        return builder.build();
    }

}
