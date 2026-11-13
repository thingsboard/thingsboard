// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.TableSortOrder;
import org.thingsboard.server.common.data.report.configuration.chart.TimeSeriesChartThreshold;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.context.chart.TsChartThresholdItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.thingsboard.server.common.data.util.DataSourceUtils.getEntityLatestValue;

@Slf4j
public class ReportUtils {

    public static final Pattern REPORT_NAME_DATE_PATTERN = Pattern.compile("%d\\{([^\\}]*)\\}");
    public static final String DEFAULT_REPORT_NAME_PATTERN = "report-%d{yyyy-MM-dd_HH:mm:ss}";
    public static final Set<String> ENTITY_TIME_FIELDS = Set.of("ts", "createdTime", "startTime", "endTime", "ackTime", "clearTime", "assignTime");
    public static final String RAW_TS_PREFIX = "rawTs_";

    public static String prepareReportName(String namePattern, Date reportDate, String timeZoneStr) {
        return prepareReportName(namePattern, reportDate, timeZoneStr, null);
    }

    public static String prepareReportName(String namePattern, Date reportDate, String timeZoneStr, String extension) {
        TimeZone timeZone = (timeZoneStr == null) ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZoneStr);
        String name = (namePattern == null || namePattern.isEmpty()) ? DEFAULT_REPORT_NAME_PATTERN : namePattern;
        Matcher matcher = REPORT_NAME_DATE_PATTERN.matcher(name);
        while (matcher.find()) {
            String toReplace = matcher.group(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat(matcher.group(1));
            dateFormat.setTimeZone(timeZone);
            String replacement = dateFormat.format(reportDate);
            name = name.replace(toReplace, replacement);
        }
        if (extension != null && !name.endsWith(extension)) {
            name = name + extension;
        }
        return name;
    }

    public static void prepareReportComponent(ReportComponent component) {
        if (component instanceof DataReportComponent dataReportComponent) {
            List<DataSource> dataSources = dataReportComponent.getDataSources();
            if (dataSources != null && !dataSources.isEmpty()) {
                for (DataSource dataSource : dataSources) {
                    if (dataSource != null) {
                        prepareDataKeys(dataSource.getDataKeys());
                        prepareDataKeys(dataSource.getLatestDataKeys());
                    }
                }
            }
        }
    }

    private static void prepareDataKeys(List<DataKey> dataKeys) {
        if (dataKeys != null && !dataKeys.isEmpty()) {
            for (DataKey dataKey : dataKeys) {
                if (dataKey != null) {
                    if (StringUtils.isBlank(dataKey.getLabel())) {
                        dataKey.setLabel(dataKey.getName());
                    }
                }
            }
        }
    }

    public static Optional<DataSource> getSingleDataSource(DataReportComponent component) {
        DataSource dataSource = null;
        if (ReportComponentType.ALARM_TABLE.equals(component.getType())) {
            dataSource = ((AlarmTableComponent) component).getAlarmSource();
        } else {
            List<DataSource> dataSources = component.getDataSources();
            if (dataSources != null && !dataSources.isEmpty()) {
                dataSource = dataSources.get(0);
            }
        }
        if (isDataSourceValid(dataSource)) {
            return Optional.of(dataSource);
        } else {
            return Optional.empty();
        }
    }

    public static List<DataSource> getMultipleDataSources(DataReportComponent component) {
        List<DataSource> dataSources = component.getDataSources();
        if (dataSources == null) {
            dataSources = new ArrayList<>();
        } else {
            dataSources = dataSources.stream().filter(ReportUtils::isDataSourceValid).toList();
        }
        return dataSources;
    }

    public static boolean isDataSourceValid(DataSource dataSource) {
        if (dataSource == null) {
            return false;
        }
        switch (dataSource.getType()) {
            case DEVICE:
                if (dataSource.getDeviceId() == null) {
                    return false;
                }
                break;
            case ENTITY:
                if (dataSource.getEntityAliasId() == null) {
                    return false;
                }
        }
        return true;
    }

    public static String updateDashboardReportStateParamsWithEntity(String state, EntityData stateEntity) {
        JsonNode stateObj = null;
        if (StringUtils.isNotBlank(state)) {
            try {
                String decoded = new String(Base64.getDecoder().decode(state));
                JsonNode parsed = JacksonUtil.toJsonNode(decoded);
                if (parsed.isArray() && !parsed.isEmpty()) {
                    stateObj = parsed;
                }
            } catch (Exception ignored) {
            }
        }
        if (stateObj == null) {
            stateObj = JacksonUtil.newArrayNode();
            ObjectNode stateData = JacksonUtil.newObjectNode();
            stateData.set("id", NullNode.getInstance());
            ((ArrayNode) stateObj).add(stateData);
        }

        ObjectNode stateParams;
        JsonNode stateData = stateObj.get(stateObj.size() - 1);
        if (stateData.has("params") && stateData.get("params").isObject()) {
            stateParams = (ObjectNode) stateData.get("params");
        } else {
            stateParams = JacksonUtil.newObjectNode();
            ((ObjectNode) stateData).set("params", stateParams);
        }
        stateParams.set("entityId", JacksonUtil.valueToTree(stateEntity.getEntityId()));
        Optional<String> entityName = getEntityLatestValue(stateEntity, EntityKeyType.ENTITY_FIELD, "name");
        Optional<String> entityLabel = getEntityLatestValue(stateEntity, EntityKeyType.ENTITY_FIELD, "label");
        stateParams.remove("entityName");
        stateParams.remove("entityLabel");
        entityName.ifPresent(s -> stateParams.put("entityName", s));
        entityLabel.ifPresent(s -> stateParams.put("entityLabel", s));
        String newStateJsonStr = JacksonUtil.toString(stateObj);
        String b64 = Base64.getEncoder().encodeToString(newStateJsonStr.getBytes(StandardCharsets.UTF_8));
        return java.net.URLEncoder.encode(b64, StandardCharsets.UTF_8);
    }

    public static List<TsChartThresholdItem> collectThresholdItems(List<TimeSeriesChartThreshold> thresholds,
                                                                   List<EntityData> entityDatas,
                                                                   boolean latestElseEntity) {
        List<TsChartThresholdItem> thresholdItems = new ArrayList<>();
        if (!thresholds.isEmpty() && !entityDatas.isEmpty()) {
            EntityData entity = entityDatas.get(0);
            Map<EntityKeyType, Map<String, TsValue>> latestValues = entity.getLatest();
            for (TimeSeriesChartThreshold threshold : thresholds) {
                EntityKeyType keyType = EntityKeyType.fromName(latestElseEntity ? threshold.getLatestKeyType() : threshold.getEntityKeyType());
                Map<String, TsValue> valuesByType = latestValues.get(keyType);
                if (valuesByType != null) {
                    TsValue tsValue = valuesByType.get(latestElseEntity ? threshold.getLatestKey() : threshold.getEntityKey());
                    if (tsValue != null) {
                        try {
                            double doubleValue = Double.parseDouble(tsValue.getValue());
                            thresholdItems.add(new TsChartThresholdItem(threshold, doubleValue));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return thresholdItems;
    }

    public static String formatValueWithPrecisionAndUnits(String value, DataKey dataKey) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            if (dataKey.getDecimals() != null) {
                BigDecimal decimal = new BigDecimal(value);
                value = decimal.setScale(dataKey.getDecimals(), RoundingMode.HALF_UP).toPlainString();
            }
        } catch (NumberFormatException | ArithmeticException ignored) {
        }
        if (dataKey.getUnits() != null) {
            value += dataKey.getUnits();
        }
        return value;
    }

    public static void sortRowsByTableSortOrder(List<Map<String, String>> rows, TableSortOrder tableSortOrder) {
        if (tableSortOrder == null || tableSortOrder.getColumn() == null || rows.isEmpty()) {
            return;
        }

        String column = tableSortOrder.getColumn();
        if (rows.get(0).containsKey(RAW_TS_PREFIX + column)) {
            column = RAW_TS_PREFIX + column; // Handle timestamp columns
        }

        String finalColumn = column;
        Comparator<Map<String, String>> comparator = Comparator.comparing(
                row -> row.getOrDefault(finalColumn, ""),
                ReportUtils::compareMixedValuesBlankFirst
        );

        if (tableSortOrder.getDirection() == TableSortOrder.Direction.DESC) {
            comparator = comparator.reversed();
        }

        rows.sort(comparator);
    }

    public static int compareMixedValuesBlankFirst(String v1, String v2) {
        boolean v1Blank = StringUtils.isBlank(v1); // treat null and empty/blank values alike
        boolean v2Blank = StringUtils.isBlank(v2);
        if (v1Blank && v2Blank) return 0;
        if (v1Blank) return -1;
        if (v2Blank) return 1;

        boolean isV1Numeric = NumberUtils.isParsable(v1);
        boolean isV2Numeric = NumberUtils.isParsable(v2);

        if (isV1Numeric && isV2Numeric) {
            return Double.compare(Double.parseDouble(v1), Double.parseDouble(v2));
        }
        if (isV1Numeric) return -1; // numbers before strings, consistently
        if (isV2Numeric) return 1;

        return v1.compareToIgnoreCase(v2);
    }

    public static Object convertStringToTypedValue(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        if (NumberUtils.isParsable(value)) {
            return Double.parseDouble(value);
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }

    public static String formatTimestamp(long timestamp, String pattern, String timezone) {
        if (timestamp == 0) {
            return "";
        }
        if (pattern == null || pattern.isEmpty() || pattern.equals("milliseconds")) {
            return String.valueOf(timestamp);
        }

        try {
            ZoneId zoneId = (timezone != null && !timezone.isBlank())
                    ? ZoneId.of(timezone)
                    : ZoneId.systemDefault();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withZone(zoneId);
            return formatter.format(Instant.ofEpochMilli(timestamp));
        } catch (Exception e) {
            return "Invalid timestamp: " + timestamp;
        }
    }

    public static String formatTimestamp(String timestampStr, String pattern, TbReportCtx ctx, String timezone) {
        try {
            long timestamp = Long.parseLong(timestampStr);
            return formatTimestamp(timestamp, pattern, ctx, timezone);
        } catch (NumberFormatException e) {
            return "Invalid timestamp string: " + timestampStr;
        }
    }

    public static String formatTimestamp(long timestamp, String pattern, TbReportCtx ctx) {
        return formatTimestamp(timestamp, pattern, ctx, null);
    }

    public static String formatTimestamp(long timestamp, String pattern, TbReportCtx ctx, String timezone) {
        String effectivePattern = (pattern != null && !pattern.isEmpty())
                ? pattern
                : ctx.getConfiguration().getTimeDataPattern();
        String targetTimezone = StringUtils.isNotBlank(timezone) ? timezone : ctx.getTimeZone();
        return formatTimestamp(timestamp, effectivePattern, targetTimezone);
    }

}
