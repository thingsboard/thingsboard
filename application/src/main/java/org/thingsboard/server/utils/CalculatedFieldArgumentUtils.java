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
package org.thingsboard.server.utils;

import lombok.NonNull;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SimpleCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.RelatedEntitiesAggregationCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.AggIntervalEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.AggIntervalEntryStatus;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.EntityAggregationArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.EntityAggregationCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.alarm.AlarmCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationCalculatedFieldState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry.DEFAULT_VERSION;

public class CalculatedFieldArgumentUtils {

    public static ArgumentEntry transformSingleValueArgument(@NonNull KvEntry kvEntry) {
        return kvEntry.getValue() != null ? ArgumentEntry.createSingleValueArgument(kvEntry) : new SingleValueArgumentEntry();
    }

    public static ArgumentEntry transformTsRollingArgument(List<TsKvEntry> tsRolling, int limit, long argTimeWindow) {
        return ArgumentEntry.createTsRollingArgument(tsRolling, limit, argTimeWindow);
    }

    public static ArgumentEntry transformAggMetricArgument(List<TsKvEntry> timeSeries, String argKey, AggMetric aggMetric) {
        if (timeSeries == null || timeSeries.isEmpty()) {
            return createDefaultMetricArgumentEntry(argKey, aggMetric);
        }
        return ArgumentEntry.createSingleValueArgument(timeSeries.get(0));
    }

    public static ArgumentEntry createDefaultMetricArgumentEntry(String argKey, AggMetric metric) {
        Long defaultValue = metric.getDefaultValue();
        if (defaultValue != null) {
            return ArgumentEntry.createSingleValueArgument(new DoubleDataEntry(argKey, defaultValue.doubleValue()));
        }
        return new SingleValueArgumentEntry();
    }

    public static ArgumentEntry transformAggregationArgument(List<TsKvEntry> timeSeries, long startIntervalTs, long endIntervalTs) {
        Map<AggIntervalEntry, AggIntervalEntryStatus> aggIntervals = new HashMap<>();
        AggIntervalEntry aggIntervalEntry = new AggIntervalEntry(startIntervalTs, endIntervalTs);
        if (timeSeries == null || timeSeries.isEmpty()) {
            aggIntervals.put(aggIntervalEntry, new AggIntervalEntryStatus());
        } else {
            aggIntervals.put(aggIntervalEntry, new AggIntervalEntryStatus(System.currentTimeMillis()));
        }
        return new EntityAggregationArgumentEntry(aggIntervals);
    }

    private static KvEntry createDefaultKvEntry(Argument argument) {
        String key = argument.getRefEntityKey().getKey();
        String defaultValue = argument.getDefaultValue();
        if (StringUtils.isBlank(defaultValue)) {
            return new StringDataEntry(key, null);
        }
        if (NumberUtils.isParsable(defaultValue)) {
            return new DoubleDataEntry(key, Double.parseDouble(defaultValue));
        }
        if ("true".equalsIgnoreCase(defaultValue) || "false".equalsIgnoreCase(defaultValue)) {
            return new BooleanDataEntry(key, Boolean.parseBoolean(defaultValue));
        }
        return new StringDataEntry(key, defaultValue);
    }

    public static TsKvEntry createDefaultTsKvEntry(Argument argument, long ts) {
        return new BasicTsKvEntry(ts, createDefaultKvEntry(argument), DEFAULT_VERSION);
    }
    public static AttributeKvEntry createDefaultAttributeEntry(Argument argument, long ts) {
        return new BaseAttributeKvEntry(createDefaultKvEntry(argument), ts, DEFAULT_VERSION);
    }

    public static CalculatedFieldState createStateByType(CalculatedFieldCtx ctx, EntityId entityId) {
        return switch (ctx.getCfType()) {
            case SIMPLE -> new SimpleCalculatedFieldState(entityId);
            case SCRIPT -> new ScriptCalculatedFieldState(entityId);
            case GEOFENCING -> new GeofencingCalculatedFieldState(entityId);
            case ALARM -> new AlarmCalculatedFieldState(entityId);
            case PROPAGATION -> new PropagationCalculatedFieldState(entityId);
            case RELATED_ENTITIES_AGGREGATION -> new RelatedEntitiesAggregationCalculatedFieldState(entityId);
            case ENTITY_AGGREGATION -> new EntityAggregationCalculatedFieldState(entityId);
        };
    }

}
