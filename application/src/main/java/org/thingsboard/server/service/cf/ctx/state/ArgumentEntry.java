// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.RelatedEntitiesArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.EntityAggregationArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationArgumentEntry;

import java.util.List;
import java.util.Map;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SingleValueArgumentEntry.class, name = "SINGLE_VALUE"),
        @JsonSubTypes.Type(value = TsRollingArgumentEntry.class, name = "TS_ROLLING"),
        @JsonSubTypes.Type(value = GeofencingArgumentEntry.class, name = "GEOFENCING"),
        @JsonSubTypes.Type(value = PropagationArgumentEntry.class, name = "PROPAGATION"),
        @JsonSubTypes.Type(value = RelatedEntitiesArgumentEntry.class, name = "RELATED_ENTITIES"),
        @JsonSubTypes.Type(value = EntityAggregationArgumentEntry.class, name = "ENTITY_AGGREGATION")
})
public interface ArgumentEntry {

    @JsonIgnore
    ArgumentEntryType getType();

    Object getValue();

    boolean updateEntry(ArgumentEntry entry, CalculatedFieldCtx ctx);

    boolean isEmpty();

    default JsonNode jsonValue() {
        return JacksonUtil.valueToTree(toTbelCfArg());
    }

    TbelCfArg toTbelCfArg();

    boolean isForceResetPrevious();

    void setForceResetPrevious(boolean forceResetPrevious);

    static ArgumentEntry createSingleValueArgument(KvEntry kvEntry) {
        return new SingleValueArgumentEntry(kvEntry);
    }

    static ArgumentEntry createSingleValueArgument(EntityId entityId, ArgumentEntry argumentEntry) {
        return new SingleValueArgumentEntry(entityId, argumentEntry);
    }

    static ArgumentEntry createTsRollingArgument(List<TsKvEntry> kvEntries, int limit, long timeWindow) {
        return new TsRollingArgumentEntry(kvEntries, limit, timeWindow);
    }

    static ArgumentEntry createGeofencingValueArgument(Map<EntityId, KvEntry> entityIdkvEntryMap) {
        return new GeofencingArgumentEntry(entityIdkvEntryMap);
    }

    static ArgumentEntry createPropagationArgument(List<EntityId> entityIds) {
        return new PropagationArgumentEntry(entityIds);
    }

    static ArgumentEntry createAggArgument(Map<EntityId, ArgumentEntry> entityIdkvEntryMap) {
        return new RelatedEntitiesArgumentEntry(entityIdkvEntryMap, false);
    }

}
