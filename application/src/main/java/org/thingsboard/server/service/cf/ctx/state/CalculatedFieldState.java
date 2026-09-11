// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.aggregation.RelatedEntitiesAggregationCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.EntityAggregationCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.alarm.AlarmCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationCalculatedFieldState;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration.PROPAGATION_CONFIG_ARGUMENT;
import static org.thingsboard.server.utils.CalculatedFieldUtils.toSingleValueArgumentProto;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(value = SimpleCalculatedFieldState.class, name = "SIMPLE"),
        @Type(value = ScriptCalculatedFieldState.class, name = "SCRIPT"),
        @Type(value = GeofencingCalculatedFieldState.class, name = "GEOFENCING"),
        @Type(value = AlarmCalculatedFieldState.class, name = "ALARM"),
        @Type(value = PropagationCalculatedFieldState.class, name = "PROPAGATION"),
        @Type(value = RelatedEntitiesAggregationCalculatedFieldState.class, name = "RELATED_ENTITIES_AGGREGATION"),
        @Type(value = EntityAggregationCalculatedFieldState.class, name = "ENTITY_AGGREGATION")
})
public interface CalculatedFieldState extends Closeable {

    @JsonIgnore
    CalculatedFieldType getType();

    EntityId getEntityId();

    Map<String, ArgumentEntry> getArguments();

    JsonNode getArgumentsJson();

    long getLatestTimestamp();

    void setCtx(CalculatedFieldCtx ctx, TbActorRef actorCtx);

    void init(boolean restored);

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

    record ReadinessStatus(boolean ready, String errorMsg) {

        private static final String MISSING_REQUIRED_ARGUMENTS_ERROR = "Required arguments are missing: ";
        private static final String MISSING_PROPAGATION_TARGETS_ERROR = "No entities found via 'Propagation path to related entities'. " +
                                                                        "Verify the configured relation type and direction.";
        private static final String MISSING_PROPAGATION_TARGETS_AND_ARGUMENTS_ERROR = MISSING_PROPAGATION_TARGETS_ERROR + " Missing arguments to propagate: ";
        public static final String MISSING_AGGREGATION_ENTITIES_ERROR = "No entities found via 'Aggregation path to related entities'. " +
                                                                        "Verify the configured relation type and direction.";
        public static final ReadinessStatus READY = new ReadinessStatus(true, null);

        public static ReadinessStatus notReady(String errorMsg) {
            return new ReadinessStatus(false, errorMsg);
        }

        public static ReadinessStatus from(List<String> emptyOrMissingArguments) {
            if (CollectionsUtil.isEmpty(emptyOrMissingArguments)) {
                return ReadinessStatus.READY;
            }
            boolean propagationCtxIsEmpty = emptyOrMissingArguments.remove(PROPAGATION_CONFIG_ARGUMENT);
            if (!propagationCtxIsEmpty) {
                return notReady(MISSING_REQUIRED_ARGUMENTS_ERROR + String.join(", ", emptyOrMissingArguments));
            }
            if (emptyOrMissingArguments.isEmpty()) {
                return notReady(MISSING_PROPAGATION_TARGETS_ERROR);
            }
            return notReady(MISSING_PROPAGATION_TARGETS_AND_ARGUMENTS_ERROR + String.join(", ", emptyOrMissingArguments));
        }
    }

}
