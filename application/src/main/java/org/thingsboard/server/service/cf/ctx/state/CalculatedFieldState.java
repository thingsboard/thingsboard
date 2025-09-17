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
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingCalculatedFieldState;

import java.util.List;
import java.util.Map;

import static org.thingsboard.server.utils.CalculatedFieldUtils.toSingleValueArgumentProto;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleCalculatedFieldState.class, name = "SIMPLE"),
        @JsonSubTypes.Type(value = ScriptCalculatedFieldState.class, name = "SCRIPT"),
        @JsonSubTypes.Type(value = GeofencingCalculatedFieldState.class, name = "GEOFENCING"),
})
public interface CalculatedFieldState {

    @JsonIgnore
    CalculatedFieldType getType();

    Map<String, ArgumentEntry> getArguments();

    long getLatestTimestamp();

    default void setDirty(boolean dirty) {
    }

    default boolean isDirty() {
        return false;
    }

    void setRequiredArguments(List<String> requiredArguments);

    boolean updateState(CalculatedFieldCtx ctx, Map<String, ArgumentEntry> argumentValues);

    ListenableFuture<CalculatedFieldResult> performCalculation(EntityId entityId, CalculatedFieldCtx ctx);

    @JsonIgnore
    boolean isReady();

    boolean isSizeExceedsLimit();

    @JsonIgnore
    default boolean isSizeOk() {
        return !isSizeExceedsLimit();
    }

    void checkStateSize(CalculatedFieldEntityCtxId ctxId, long maxStateSize);

    default void checkArgumentSize(String name, ArgumentEntry entry, CalculatedFieldCtx ctx) {
        if (entry instanceof TsRollingArgumentEntry || entry instanceof GeofencingArgumentEntry) {
            return;
        }
        if (entry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
            if (ctx.getMaxSingleValueArgumentSize() > 0 && toSingleValueArgumentProto(name, singleValueArgumentEntry).getSerializedSize() > ctx.getMaxSingleValueArgumentSize()) {
                throw new IllegalArgumentException("Single value size exceeds the maximum allowed limit. The argument will not be used for calculation.");
            }
        }
    }

}
