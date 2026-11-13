// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context.chart;

import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.chart.ComparisonDuration;
import org.thingsboard.server.common.data.report.configuration.chart.DataKeyComparisonSettings;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartKeySettings;
import org.thingsboard.server.common.data.report.configuration.timewindow.Interval;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
public class TsChartDataSource {

    private final boolean generated;
    private final boolean comparison;
    private final EntityId entityId;
    private final EntityData entityData;
    private String entityName;
    private String entityLabel;
    private final List<DataKey> dataKeys;
    private final List<TsChartSeriesData> data;
    private final int index;
    private final Map<String, Object> variables;

    public TsChartDataSource(DataSource dataSource,
                             EntityData entityData,
                             List<TsKvEntry> tsKvEntries,
                             TimeIntervalCalculator.TimeRange timeWindow,
                             Interval aggInterval,
                             Aggregation aggregation,
                             ZoneId zoneId,
                             boolean comparison,
                             ComparisonDuration timeForComparison,
                             DataPostProcessFunction postProcessFunction,
                             int index,
                             int startDataIndex) {
        List<DataKey> newDataKeys = new ArrayList<>();
        for (DataKey dataKey : dataSource.getDataKeys()) {
            if (!comparison || dataKey.isComparisonKey()) {
                newDataKeys.add(JacksonUtil.clone(dataKey));
            }
        }
        this.dataKeys = newDataKeys;
        this.index = index;
        this.generated = index > 0;
        this.comparison = comparison;
        this.entityId = entityData.getEntityId();
        this.entityData = entityData;
        this.entityName = "";
        this.entityLabel = "";
        this.variables = new HashMap<>();
        if (entityData.getLatest() != null && entityData.getLatest().get(EntityKeyType.ENTITY_FIELD) != null) {
            Map<String, TsValue> entityFields = entityData.getLatest().get(EntityKeyType.ENTITY_FIELD);
            TsValue nameValue = entityFields.get("name");
            if (nameValue != null) {
                this.entityName = nameValue.getValue();
            }
            TsValue labelValue = entityFields.get("label");
            if (labelValue != null) {
                this.entityLabel = labelValue.getValue();
            }
        }
        this.data = new ArrayList<>(getDataKeys().size());
        int dataIndex = startDataIndex;
        for (int keyIndex = 0; keyIndex < dataKeys.size(); keyIndex++) {
            DataKey key = this.dataKeys.get(keyIndex);
            if (comparison) {
                TimeSeriesChartKeySettings timeSeriesChartKeySettings = (TimeSeriesChartKeySettings)key.getSettings();
                DataKeyComparisonSettings comparisonSettings = timeSeriesChartKeySettings.getComparisonSettings();
                if (StringUtils.isNotBlank(comparisonSettings.getComparisonValuesLabel())) {
                    key.setLabel(comparisonSettings.getComparisonValuesLabel());
                } else {
                    String label = key.getLabel();
                    label += " " + labelSuffixForComparisonUnit(timeForComparison);
                    key.setLabel(label);
                }
            }
            TsChartSeriesData seriesData = new TsChartSeriesData();
            seriesData.setDataSource(this);
            seriesData.setDataKey(key);
            List<TsChartSeriesEntry> keyValues = tsKvEntries.stream().filter(entry -> entry.getKey().equals(key.getName()))
                    .map(
                            entry -> {
                                TimeIntervalCalculator.TimeRange interval =
                                        TimeIntervalCalculator.getAggTimeRange(timeWindow, aggInterval, aggregation, zoneId, entry.getTs());
                                long ts = interval.startTs + (long)Math.floor((double)(interval.endTs - interval.startTs) / 2f);
                                String value = entry.getValueAsString();
                                Object processed = postProcessFunction.apply(key, ts, value);
                                value = processed != null ? processed.toString() : null;
                                Double doubleValue = null;
                                if (value != null) {
                                    try {
                                        doubleValue = Double.parseDouble(value);
                                    } catch (NumberFormatException ignored) {}
                                }
                                return new TsChartSeriesEntry(ts, interval, value, doubleValue);
                            }
                    ).filter(entry -> entry.getValue() != null).sorted(Comparator.comparing(TsChartSeriesEntry::getTs)).toList();
            seriesData.setData(keyValues);
            seriesData.setNumericData(keyValues.stream().map(TsChartSeriesEntry::getDoubleValue).filter(Objects::nonNull).toList());
            seriesData.setIndex(dataIndex);
            seriesData.setKeyIndex(keyIndex);
            this.data.add(seriesData);
            dataIndex++;
        }
        this.variables.put("entityName", this.entityName);
        this.variables.put("entityLabel", this.entityLabel);
    }

    private String labelSuffixForComparisonUnit(ComparisonDuration timeUnit) {
        switch (timeUnit) {
            case previousInterval -> {
                return "(previous interval)";
            }
            case days -> {
                return "(day ago)";
            }
            case weeks -> {
                return "(week ago)";
            }
            case months -> {
                return "(month ago)";
            }
            case years -> {
                return "(year ago)";
            }
            case customInterval -> {
                return "(custom interval)";
            }
            default -> {
                return "(unknown)";
            }
        }
    }

}
