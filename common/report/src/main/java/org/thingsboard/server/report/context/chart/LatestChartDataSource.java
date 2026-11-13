// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context.chart;

import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class LatestChartDataSource {

    private final boolean generated;
    private final EntityId entityId;
    private final EntityData entityData;
    private String entityName;
    private String entityLabel;
    private final List<DataKey> dataKeys;
    private final List<LatestChartDataItem> items;
    private final int index;
    private final Map<String, Object> variables;

    public LatestChartDataSource(DataSource dataSource,
                                 EntityData entityData,
                                 DataPostProcessFunction postProcessFunction,
                                 int index) {
        List<DataKey> newDataKeys = new ArrayList<>();
        for (DataKey dataKey : dataSource.getDataKeys()) {
            newDataKeys.add(JacksonUtil.clone(dataKey));
        }
        this.dataKeys = newDataKeys;
        this.index = index;
        this.generated = index > 0;
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

        this.variables.put("entityName", this.entityName);
        this.variables.put("entityLabel", this.entityLabel);

        this.items = new ArrayList<>(getDataKeys().size());
        int dataIndex = this.index * this.dataKeys.size();
        for (int keyIndex = 0; keyIndex < dataKeys.size(); keyIndex++) {
            DataKey key = this.dataKeys.get(keyIndex);
            LatestChartDataItem dataItem = new LatestChartDataItem();
            dataItem.setDataSource(this);
            dataItem.setDataKey(key);
            putDataForDataKey(key, dataItem, postProcessFunction);
            dataItem.setIndex(dataIndex);
            dataItem.setKeyIndex(keyIndex);
            String label = ThymeleafUtil.renderFromTextString(key.getLabel(), this.variables);
            dataItem.setLabel(label);
            this.items.add(dataItem);
            dataIndex++;
        }
    }

    private void putDataForDataKey(DataKey dataKey, LatestChartDataItem dataItem, DataPostProcessFunction postProcessFunction) {
        dataItem.setHasValue(false);
        dataItem.setValue(0);
        TsValue tsValue = extractTsValueForDataKey(dataKey);
        if (tsValue != null) {
            dataItem.setTs(tsValue.getTs());
            String value = tsValue.getValue();
            Object processed = postProcessFunction.apply(dataKey, dataItem.getTs(), value);
            value = processed != null ? processed.toString() : null;
            if (value != null) {
                try {
                    double doubleValue = Double.parseDouble(value);
                    dataItem.setHasValue(true);
                    dataItem.setValue(doubleValue);
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    private TsValue extractTsValueForDataKey(DataKey dataKey) {
        if (dataKey.getAggregationType() != null && dataKey.getAggregationType() != Aggregation.NONE) {
            if (this.entityData.getTimeseries() != null) {
                Map<String, TsValue[]> timeseries = this.entityData.getTimeseries();
                TsValue[] values = timeseries.get(dataKey.getLabel());
                if (values != null && values.length > 0) {
                    return values[0];
                }
            }
        } else if (this.entityData.getLatest() != null) {
            Map<EntityKeyType, Map<String, TsValue>> latest = this.entityData.getLatest();
            Map<String, TsValue> keyValueMap = latest.get(EntityKeyType.fromName(dataKey.getType()));
            if (keyValueMap != null) {
                return keyValueMap.get(dataKey.getName());
            }
        }
        return null;
    }
}
