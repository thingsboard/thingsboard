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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.aggregation.RelatedEntitiesAggregationCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.alarm.AlarmCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationCalculatedFieldState;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.utils.CalculatedFieldUtils.toSingleValueArgumentProto;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(value = SimpleCalculatedFieldState.class, name = "SIMPLE"),
        @Type(value = ScriptCalculatedFieldState.class, name = "SCRIPT"),
        @Type(value = GeofencingCalculatedFieldState.class, name = "GEOFENCING"),
        @Type(value = AlarmCalculatedFieldState.class, name = "ALARM"),
        @Type(value = PropagationCalculatedFieldState.class, name = "PROPAGATION"),
        @Type(value = RelatedEntitiesAggregationCalculatedFieldState.class, name = "RELATED_ENTITIES_AGGREGATION")
})
public interface CalculatedFieldState extends Closeable {

    @JsonIgnore
    CalculatedFieldType getType();

    EntityId getEntityId();

    Map<String, ArgumentEntry> getArguments();

    long getLatestTimestamp();

    void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx);

    void init();

    Map<String, ArgumentEntry> update(Map<String, ArgumentEntry> arguments, CalculatedFieldCtx ctx);

    void reset();

    ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) throws Exception;

    @JsonIgnore
    boolean isReady();

    ReadinessStatus getReadinessStatus();

    boolean isSizeExceedsLimit();

    @JsonIgnore
    default boolean isSizeOk() {
        return !isSizeExceedsLimit();
    }

    TopicPartitionInfo getPartition();

    void setPartition(TopicPartitionInfo partition);

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

    @Data
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    class ReadinessStatus {

        private List<String> emptyArguments;

        public boolean isReady() {
            return emptyArguments == null || emptyArguments.isEmpty();
        }

        public String stringValue() {
            return JacksonUtil.toString(this);
        }


    }

}
