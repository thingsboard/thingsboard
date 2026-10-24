// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityOwnerFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getTimeRange;
import static org.thingsboard.server.common.data.util.DataSourceUtils.getAlarmLatestValue;
import static org.thingsboard.server.common.data.util.DataSourceUtils.getEntityLatestValue;
import static org.thingsboard.server.report.util.ReportQueryUtils.DEFAULT_SORT_ORDER;
import static org.thingsboard.server.report.util.ReportQueryUtils.buildEntityFilter;
import static org.thingsboard.server.report.util.ReportQueryUtils.toAlarmDataQuery;
import static org.thingsboard.server.report.util.ReportQueryUtils.toEntityDataQuery;
import static org.thingsboard.server.report.util.ReportUtils.ENTITY_TIME_FIELDS;
import static org.thingsboard.server.report.util.ReportUtils.RAW_TS_PREFIX;
import static org.thingsboard.server.report.util.ReportUtils.convertStringToTypedValue;
import static org.thingsboard.server.report.util.ReportUtils.formatTimestamp;
import static org.thingsboard.server.report.util.ReportUtils.formatValueWithPrecisionAndUnits;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;

@Slf4j
public abstract class AbstractReportService implements ReportService {

    public static final int DEFAULT_ENTITIES_PAGE_SIZE = 1024;

    private static final Map<String, String> ALARM_FIELD_ALIASES_MAP = Map.of(
            "startTime", "startTs",
            "endTime", "endTs",
            "ackTime", "ackTs",
            "clearTime", "clearTs",
            "assignTime", "assignTs",
            "originator", "originatorName",
            "originatorType", "originator.entityType"
    );

    @Lazy
    @Autowired
    protected ReportDataService dataService;

    protected List<EntityData> fetchEntities(TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        return fetchEntities(ctx, dataSource, stateEntityId, DEFAULT_SORT_ORDER);
    }

    protected List<EntityData> fetchEntities(TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId, EntityDataSortOrder sortOrder) {
        return fetchEntities(ctx, dataSource, stateEntityId, sortOrder, false);
    }

    protected List<EntityData> fetchEntities(TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId, EntityDataSortOrder sortOrder, boolean singleEntity) {
        EntityFilter filter = buildEntityFilter(dataSource, ctx, stateEntityId);
        if (filter instanceof SingleEntityFilter singleEntityFilter && singleEntityFilter.getSingleEntity() == null) {
            return Collections.emptyList();
        }
        if (filter instanceof StateEntityOwnerFilter stateEntityOwnerFilter && stateEntityOwnerFilter.getSingleEntity() == null) {
            return Collections.emptyList();
        } // TODO: add black-box tests for entity filters
        return fetchEntityDataByQuery(pageLink -> toEntityDataQuery(dataSource, ctx, filter, pageLink, sortOrder), dataSource, ctx, singleEntity);
    }

    private List<EntityData> fetchEntityDataByQuery(Function<PageLink, EntityDataQuery> querySupplier, DataSource dataSource, TbReportCtx ctx, boolean singleEntity) {
        List<DataKey> dataKeysWithAggr = getDataKeysWithAggr(dataSource);
        List<EntityData> data = new ArrayList<>();
        Iterable<EntityData> entityDataIterable;
        if (singleEntity) {
            PageLink singleEntityPageLink = new PageLink(1);
            PageData<EntityData> entityData = dataService.findEntityDataByQuery(querySupplier.apply(singleEntityPageLink), ctx);
            entityDataIterable = entityData.getData();
        } else {
            entityDataIterable = new PageDataIterable<>(link -> dataService.findEntityDataByQuery(querySupplier.apply(link), ctx), DEFAULT_ENTITIES_PAGE_SIZE);
        }
        for (EntityData entityData : entityDataIterable) {
            updateWithAggregatedData(ctx, dataKeysWithAggr, entityData);
            data.add(entityData);
        }
        return data;
    }

    private List<DataKey> getDataKeysWithAggr(DataSource dataSource) {
        List<DataKey> dataKeys = dataSource.getDataKeys();
        if (dataKeys != null) {
            return dataKeys.stream()
                    .filter(dataKey -> (dataKey.getAggregationType() != null && dataKey.getAggregationType() != Aggregation.NONE))
                    .toList();
        }
        return Collections.emptyList();
    }

    private void updateWithAggregatedData(TbReportCtx ctx, List<DataKey> dataKeys, EntityData entityData) {
        if (!dataKeys.isEmpty()) {
            List<BaseReadTsKvQuery> queries = buildReadTsKvQueries(ctx, dataKeys);
            List<ReadTsKvQueryResult> result = dataService.findTimeseriesByQueries(entityData.getEntityId(), queries, ctx);
            int i = 0;
            for (ReadTsKvQueryResult queryResult : result) {
                List<TsKvEntry> queryResultData = queryResult.getData();
                if (CollectionUtils.isNotEmpty(queryResultData)) {
                    entityData.getTimeseries().put(dataKeys.get(i).getLabel(), queryResult.toTsValues());
                }
                i++;
            }
        }
    }

    private List<BaseReadTsKvQuery> buildReadTsKvQueries(TbReportCtx ctx, List<DataKey> dataKeysWithAggregation) {
        List<BaseReadTsKvQuery> queries = new ArrayList<>();
        for (DataKey key : dataKeysWithAggregation) {
            TimeWindowConfiguration timeWindowConf = key.getTimewindow();
            String targetTimezone = StringUtils.isNotBlank(timeWindowConf.getTimezone()) ?
                    timeWindowConf.getTimezone() : ctx.getTimeZone();
            TimeIntervalCalculator.TimeRange timeRange = getTimeRange(timeWindowConf, targetTimezone);
            var query = new BaseReadTsKvQuery(key.getName(), timeRange.startTs, timeRange.endTs, timeRange.endTs - timeRange.startTs, 1, key.getAggregationType());
            queries.add(query);
        }
        return queries;
    }

    protected List<Map<String, String>> collectEntityDatas(TbReportCtx ctx, DataSource dataSource, EntityId stateEntityId) {
        return switch (dataSource.getType()) {
            case DEVICE, ENTITY -> fetchEntities(ctx, dataSource, stateEntityId)
                    .stream()
                    .map(entityData -> toStringMap(entityData, dataSource.getDataKeys(), ctx, null))
                    .collect(Collectors.toList());
            default -> throw new IllegalArgumentException("Unknown data source type: " + dataSource.getType());
        };
    }

    protected ComponentData buildTsComponentData(int usablePageWidthPx, TbReportCtx ctx, TimeseriesTableComponent component, EntityData entity) {
        TimeWindowConfiguration timeWindowConf = component.getTimewindow();
        History historyConf = timeWindowConf.getHistory();
        String targetTimezone = StringUtils.isNotBlank(timeWindowConf.getTimezone()) ?
                timeWindowConf.getTimezone() : ctx.getTimeZone();
        TimeIntervalCalculator.TimeRange timeRange = getTimeRange(timeWindowConf, targetTimezone);

        Optional<DataSource> singleDataSource = getSingleDataSource(component);
        if (singleDataSource.isEmpty()) {
            return new ComponentData(usablePageWidthPx);
        }
        List<DataKey> dataKeys = singleDataSource.get().getDataKeys();
        List<DataKey> latestDataKeys = singleDataSource.get().getLatestDataKeys();

        List<String> keys = dataKeys.stream().map(DataKey::getName).distinct().toList();
        List<TsKvEntry> result = dataService.getTimeseries(entity.getEntityId(), keys, timeRange.startTs, timeRange.endTs,
                historyConf.getInterval(), timeWindowConf.getTimezone(), timeWindowConf.getAggregation().getType(), SortOrder.Direction.DESC,
                timeWindowConf.getAggregation().getLimit(), false, ctx);
        SortOrder sortOrder = SortOrder.of("rawTs", SortOrder.Direction.DESC);
        List<Map<String, String>> entityDatas = collectTsData(dataKeys, latestDataKeys, entity, result, component, sortOrder, timeWindowConf.getTimezone(), ctx);
        Map<String, Object> variables = new HashMap<>(toStringMap(entity, dataKeys, ctx, timeWindowConf.getTimezone()));
        return new ComponentData(usablePageWidthPx, null, entityDatas, variables);
    }

    protected ComponentData buildAlarmComponentData(int usablePageWidthPx, TbReportCtx ctx, AlarmTableComponent component, EntityData stateEntity) {
        DataSource alarmSource = component.getAlarmSource();
        if (alarmSource == null) {
            return new ComponentData(usablePageWidthPx);
        }
        switch (alarmSource.getType()) {
            case DEVICE:
                if (alarmSource.getDeviceId() == null) {
                    return new ComponentData(usablePageWidthPx);
                }
                break;
            case ENTITY:
                if (alarmSource.getEntityAliasId() == null) {
                    return new ComponentData(usablePageWidthPx);
                }
                break;
            default:
                return new ComponentData(usablePageWidthPx);
        }
        List<DataKey> alarmDataKeys = alarmSource.getDataKeys()
                .stream()
                .filter(dataKey -> dataKey.getType().equals("alarm"))
                .collect(Collectors.toList());
        List<DataKey> latestDataKeys = alarmSource.getDataKeys()
                .stream()
                .filter(dataKey -> !dataKey.getType().equals("alarm"))
                .collect(Collectors.toList());
        EntityId stateEntityId = stateEntity != null ? stateEntity.getEntityId() : null;
        List<EntityData> entityDataList = fetchEntities(ctx, alarmSource, stateEntityId);
        Map<EntityId, EntityData> entityDataMap = entityDataList.stream()
                .collect(Collectors.toMap(EntityData::getEntityId, Function.identity()));
        List<Map<String, String>> entityDatas = new ArrayList<>();
        if (entityDataMap.size() == 0) {
            return new ComponentData(usablePageWidthPx);
        }
        for (AlarmData alarmData : new PageDataIterable<>(link -> dataService.findAlarmDataByQueryForEntities(toAlarmDataQuery(component, ctx, stateEntityId, link), entityDataMap.keySet(), ctx), 1024)) {
            Map<String, String> mergedData = toStringMap(alarmData, alarmDataKeys, ctx, component.getTimewindow().getTimezone());
            EntityData entityData = entityDataMap.get(alarmData.getEntityId());
            mergedData.putAll(toStringMap(entityData, latestDataKeys, ctx, component.getTimewindow().getTimezone()));
            entityDatas.add(mergedData);
        }
        Map<String, String> variables = new HashMap<>();
        if (stateEntity != null) {
            putEntityInfoData(stateEntity, variables);
        } else {
            putEntityInfoData(entityDataList.get(0), variables);
        }
        return new ComponentData(usablePageWidthPx, null, entityDatas, new HashMap<>(variables));
    }

    protected Map<String, String> toStringMap(EntityData entityData, List<DataKey> dataKeys, TbReportCtx ctx, String timezone) {
        HashMap<String, String> data = new HashMap<>();
        if (entityData != null) {
            putLatestValues(dataKeys, data, entityData.getLatest(), ctx, timezone);
            putAggregatedTsValues(dataKeys, data, entityData.getTimeseries(), ctx, timezone);
            putEntityInfoData(entityData, data);
        }
        return data;
    }

    protected void putEntityInfoData(EntityData entityData, Map<String, String> data) {
        Optional<String> entityName = getEntityLatestValue(entityData, EntityKeyType.ENTITY_FIELD, "name");
        Optional<String> entityLabel = getEntityLatestValue(entityData, EntityKeyType.ENTITY_FIELD, "label");
        data.put("entityName", entityName.orElse(""));
        data.put("entityLabel", entityLabel.orElse(""));
        data.put("id", entityData.getEntityId().toString());
    }

    protected Map<String, String> toStringMap(AlarmData alarmData, List<DataKey> alarmDataKeys, TbReportCtx ctx, String timezone) {
        Map<String, String> data = new HashMap<>();
        JsonNode alarmDataJson = JacksonUtil.valueToTree(alarmData);

        for (DataKey alarmKey : alarmDataKeys) {
            String targetKey = alarmKey.getName();
            String value = null;
            if (targetKey.equals("assignee")) {
                value = getAssigneeDisplayName(alarmData);
            } else {
                targetKey = ALARM_FIELD_ALIASES_MAP.getOrDefault(targetKey, targetKey);
                JsonNode jsonValue = JacksonUtil.getByKeyPath(alarmDataJson, targetKey);
                if (jsonValue != null) {
                    value = jsonValue.asText();
                }
            }
            if (value != null) {
                if (ENTITY_TIME_FIELDS.contains(alarmKey.getName())) {
                    data.put(RAW_TS_PREFIX + alarmKey.getLabel(), value);
                }
                data.put(alarmKey.getLabel(), formatValue(ctx, alarmKey, 0, value, timezone));
            }
        }
        Optional<String> entityName = getAlarmLatestValue(alarmData, EntityKeyType.ENTITY_FIELD, "name");
        Optional<String> entityLabel = getAlarmLatestValue(alarmData, EntityKeyType.ENTITY_FIELD, "label");
        data.put("entityName", entityName.orElse(""));
        data.put("entityLabel", entityLabel.orElse(""));
        return data;
    }

    protected List<EntityData> getSubReportEntities(TbReportCtx ctx, DataReportComponent component, EntityId stateEntityId) {
        Optional<DataSource> dataSource = getSingleDataSource(component);
        List<EntityData> entities;
        if (dataSource.isEmpty()) {
            entities = new ArrayList<>();
            entities.add(null);
        } else {
            entities = fetchEntities(ctx, dataSource.get(), stateEntityId);
        }
        return entities;
    }

    protected void populateReportVars(ComponentData componentData, TbReportCtx ctx) {
        Map<String, Object> variables = componentData.getVariables();
        variables.put("reportCreatedTime", ctx.getReportCreatedTime());
    }

    private void putLatestValues(List<DataKey> dataKeys, Map<String, String> data, Map<EntityKeyType, Map<String, TsValue>> latest, TbReportCtx ctx, String timezone) {
        if (dataKeys == null) {
            return;
        }
        for (DataKey dataKey : dataKeys) {
            var agg = dataKey.getAggregationType();
            if (agg != null && agg != Aggregation.NONE) {
                continue;
            }
            Map<String, TsValue> keyValueMap = latest.get(EntityKeyType.fromName(dataKey.getType()));
            if (keyValueMap != null) {
                TsValue tsValue = keyValueMap.get(dataKey.getName());
                if (tsValue != null && tsValue.getValue() != null) {
                    if (ENTITY_TIME_FIELDS.contains(dataKey.getName())) {
                        data.put(RAW_TS_PREFIX + dataKey.getLabel(), tsValue.getValue());
                    }
                    data.put(dataKey.getLabel(), formatValue(ctx, dataKey, tsValue.getTs(), tsValue.getValue(), timezone));
                }
            }
        }
    }

    private void putAggregatedTsValues(List<DataKey> dataKeys, HashMap<String, String> data, Map<String, TsValue[]> timeseries, TbReportCtx ctx, String timezone) {
        if (dataKeys == null || timeseries == null) {
            return;
        }
        for (DataKey dk : dataKeys) {
            var agg = dk.getAggregationType();
            if (agg == null || agg == Aggregation.NONE) {
                continue;
            }

            TsValue[] series = timeseries.get(dk.getLabel());
            if (series == null || series.length == 0) {
                continue;
            }
            TsValue latest = Arrays.stream(series)
                    .max(Comparator.comparingLong(TsValue::getTs))
                    .get();
            data.put(dk.getLabel(), formatValue(ctx, dk, latest.getTs(), latest.getValue(), timezone));
        }
    }

    private String getAssigneeDisplayName(AlarmData alarmData) {
        if (alarmData.getAssignee() != null) {
            return alarmData.getAssignee().getTitle();
        } else if (alarmData.getAssigneeId() != null) {
            return "User deleted";
        } else {
            return "Unassigned";
        }
    }

    private List<Map<String, String>> collectTsData(List<DataKey> dataKeys, List<DataKey> latestDataKeys, EntityData entity,
                                                    List<TsKvEntry> tsKvEntries, TimeseriesTableComponent component,
                                                    SortOrder sortOrder, String timezone, TbReportCtx ctx) {
        Optional<String> entityName = getEntityLatestValue(entity, EntityKeyType.ENTITY_FIELD, "name");
        Optional<String> entityLabel = getEntityLatestValue(entity, EntityKeyType.ENTITY_FIELD, "label");
        List<Map<String, String>> tsData = new ArrayList<>();

        Comparator<Long> tsComparator = sortOrder.getDirection() == SortOrder.Direction.ASC
                ? Comparator.naturalOrder()
                : Comparator.reverseOrder();

        Map<Long, List<TsKvEntry>> groupedByTs = tsKvEntries.stream()
                .collect(Collectors.groupingBy(
                        TsKvEntry::getTs,
                        () -> new TreeMap<>(tsComparator),
                        Collectors.toList()
                ));

        groupedByTs.forEach((ts, entries) -> {
            Map<String, String> tsValues = new LinkedHashMap<>();
            tsValues.put("rawTs", ts.toString());
            if (component.isShowTimestamp()) {
                tsValues.put(component.getTimestampLabel(), formatTimestamp(ts, component.getTimestampPattern(), ctx, timezone));
            }
            for (DataKey dataKey : dataKeys) {
                entries.stream().filter(tsKvEntry -> tsKvEntry.getKey().equals(dataKey.getName()))
                        .findFirst()
                        .ifPresentOrElse(tsKvEntry -> tsValues.put(dataKey.getLabel(), formatValue(ctx, dataKey, tsKvEntry.getTs(), tsKvEntry.getValue(), false, timezone)),
                                () -> tsValues.put(dataKey.getLabel(), formatValue(ctx, dataKey, 0, null, false, timezone)));
            }
            putLatestValues(latestDataKeys, tsValues, entity.getLatest(), ctx, timezone);
            tsValues.put("entityName", entityName.orElse(""));
            tsValues.put("entityLabel", entityLabel.orElse(""));
            tsData.add(tsValues);
        });
        return tsData;
    }

    private String formatValue(TbReportCtx ctx, DataKey dataKey, long timestamp, Object value, String timeZone) {
        return formatValue(ctx, dataKey, timestamp, value, true, timeZone);
    }

    private String formatValue(TbReportCtx ctx, DataKey dataKey, long timestamp, Object value, boolean parseString, String timezone) {
        if (dataKey == null) {
            return value != null ? value.toString() : null;
        }

        if (ENTITY_TIME_FIELDS.contains(dataKey.getName()) && !dataKey.isUsePostProcessing() && value != null) {
            value = formatTimestamp(value.toString(), ctx.getConfiguration().getTimeDataPattern(), ctx, timezone);
        }

        Object processed = postProcess(ctx, dataKey, timestamp, value, parseString);
        if (processed == null) {
            return null;
        }

        String formattedValue = processed.toString();
        if (dataKey.getDecimals() != null || dataKey.getUnits() != null) {
            formattedValue = formatValueWithPrecisionAndUnits(formattedValue, dataKey);
        }
        return formattedValue;
    }

    protected Object postProcess(TbReportCtx ctx, DataKey dataKey, long timestamp, Object value, boolean parseString) {
        if (dataKey.isUsePostProcessing()) {
            Object input = parseString && value instanceof String ? convertStringToTypedValue((String) value) : value;
            UUID scriptId = ctx.getScripts().computeIfAbsent(dataKey.getPostFuncBody(), s -> evalScript(ctx, s));
            if (scriptId != null) {
                return evalData(ctx, timestamp, input, scriptId);
            }
        }
        return value;
    }

    private Object evalData(TbReportCtx ctx, long timestamp, Object value, UUID scriptId) {
        try {
            return ctx.getTbelInvokeService().invokeScript(ctx.getTenantId(), null, scriptId, timestamp, value).get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to evaluate data: " + value, e);
        } catch (ExecutionException e) {
            String error = "Failed to evaluate data: " + value;
            log.error("[{}] {}", ctx.getTenantId(), error, e);
            return error;
        }
    }

    private UUID evalScript(TbReportCtx ctx, String script) {
        try {
            return ctx.getTbelInvokeService().eval(ctx.getTenantId(), ScriptType.REPORT_DATA_KEY_SCRIPT, script, "time", "value").get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to compile script: " + script, e);
        } catch (ExecutionException e) {
            log.error("[{}] Failed to compile script {} ", ctx.getTenantId(), script, e);
            return null;
        }
    }
}
