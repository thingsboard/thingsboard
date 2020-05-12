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
package org.thingsboard.server.dao.timeseries;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.nosql.TbResultSet;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 20.02.17.
 */
@Slf4j
public class AggregatePartitionsFunction implements com.google.common.util.concurrent.AsyncFunction<List<TbResultSet>, Optional<TsKvEntry>> {

    private static final int LONG_CNT_POS = 0;
    private static final int DOUBLE_CNT_POS = 1;
    private static final int BOOL_CNT_POS = 2;
    private static final int STR_CNT_POS = 3;
    private static final int JSON_CNT_POS = 4;
    private static final int LONG_POS = 5;
    private static final int DOUBLE_POS = 6;
    private static final int BOOL_POS = 7;
    private static final int STR_POS = 8;
    private static final int JSON_POS = 9;

    private final Aggregation aggregation;
    private final String key;
    private final long ts;
    private final Executor executor;

    public AggregatePartitionsFunction(Aggregation aggregation, String key, long ts, Executor executor) {
        this.aggregation = aggregation;
        this.key = key;
        this.ts = ts;
        this.executor = executor;
    }

    @Override
    public ListenableFuture<Optional<TsKvEntry>> apply(@Nullable List<TbResultSet> rsList) {
    log.trace("[{}][{}][{}] Going to aggregate data", key, ts, aggregation);
    if (rsList == null || rsList.isEmpty()) {
        return Futures.immediateFuture(Optional.empty());
    }
    return Futures.transform(
        Futures.allAsList(
            rsList.stream().map(rs -> rs.allRows(this.executor))
                .collect(Collectors.toList())),
        rowsList -> {
            try {
                AggregationResult aggResult = new AggregationResult();
                for (List<Row> rs : rowsList) {
                    for (Row row : rs) {
                        processResultSetRow(row, aggResult);
                    }
                }
                return processAggregationResult(aggResult);
            } catch (Exception e) {
                log.error("[{}][{}][{}] Failed to aggregate data", key, ts, aggregation, e);
                return Optional.empty();
            }
        }, this.executor);
    }

    private void processResultSetRow(Row row, AggregationResult aggResult) {
        long curCount = 0L;

        Long curLValue = null;
        Double curDValue = null;
        Boolean curBValue = null;
        String curSValue = null;
        String curJValue = null;

        long longCount = row.getLong(LONG_CNT_POS);
        long doubleCount = row.getLong(DOUBLE_CNT_POS);
        long boolCount = row.getLong(BOOL_CNT_POS);
        long strCount = row.getLong(STR_CNT_POS);
        long jsonCount = row.getLong(JSON_CNT_POS);

        if (longCount > 0 || doubleCount > 0) {
            if (longCount > 0) {
                aggResult.dataType = DataType.LONG;
                curCount += longCount;
                curLValue = getLongValue(row);
            }
            if (doubleCount > 0) {
                aggResult.hasDouble = true;
                aggResult.dataType = DataType.DOUBLE;
                curCount += doubleCount;
                curDValue = getDoubleValue(row);
            }
        } else if (boolCount > 0) {
            aggResult.dataType = DataType.BOOLEAN;
            curCount = boolCount;
            curBValue = getBooleanValue(row);
        } else if (strCount > 0) {
            aggResult.dataType = DataType.STRING;
            curCount = strCount;
            curSValue = getStringValue(row);
        } else if (jsonCount > 0) {
            aggResult.dataType = DataType.JSON;
            curCount = jsonCount;
            curJValue = getJsonValue(row);
        } else {
            return;
        }

        if (aggregation == Aggregation.COUNT) {
            aggResult.count += curCount;
        } else if (aggregation == Aggregation.AVG || aggregation == Aggregation.SUM) {
            processAvgOrSumAggregation(aggResult, curCount, curLValue, curDValue);
        } else if (aggregation == Aggregation.MIN) {
            processMinAggregation(aggResult, curLValue, curDValue, curBValue, curSValue, curJValue);
        } else if (aggregation == Aggregation.MAX) {
            processMaxAggregation(aggResult, curLValue, curDValue, curBValue, curSValue, curJValue);
        }
    }

    private void processAvgOrSumAggregation(AggregationResult aggResult, long curCount, Long curLValue, Double curDValue) {
        aggResult.count += curCount;
        if (curDValue != null) {
            aggResult.dValue = aggResult.dValue == null ? curDValue : aggResult.dValue + curDValue;
        }
        if (curLValue != null) {
            aggResult.lValue = aggResult.lValue == null ? curLValue : aggResult.lValue + curLValue;
        }
    }

    private void processMinAggregation(AggregationResult aggResult, Long curLValue, Double curDValue, Boolean curBValue, String curSValue, String curJValue) {
        if (curDValue != null || curLValue != null) {
            if (curDValue != null) {
                aggResult.dValue = aggResult.dValue == null ? curDValue : Math.min(aggResult.dValue, curDValue);
            }
            if (curLValue != null) {
                aggResult.lValue = aggResult.lValue == null ? curLValue : Math.min(aggResult.lValue, curLValue);
            }
        } else if (curBValue != null) {
            aggResult.bValue = aggResult.bValue == null ? curBValue : aggResult.bValue && curBValue;
        } else if (curSValue != null && (aggResult.sValue == null || curSValue.compareTo(aggResult.sValue) < 0)) {
            aggResult.sValue = curSValue;
        } else if (curJValue != null && (aggResult.jValue == null || curJValue.compareTo(aggResult.jValue) < 0)) {
            aggResult.jValue = curJValue;
        }
    }

    private void processMaxAggregation(AggregationResult aggResult, Long curLValue, Double curDValue, Boolean curBValue, String curSValue, String curJValue) {
        if (curDValue != null || curLValue != null) {
            if (curDValue != null) {
                aggResult.dValue = aggResult.dValue == null ? curDValue : Math.max(aggResult.dValue, curDValue);
            }
            if (curLValue != null) {
                aggResult.lValue = aggResult.lValue == null ? curLValue : Math.max(aggResult.lValue, curLValue);
            }
        } else if (curBValue != null) {
            aggResult.bValue = aggResult.bValue == null ? curBValue : aggResult.bValue || curBValue;
        } else if (curSValue != null && (aggResult.sValue == null || curSValue.compareTo(aggResult.sValue) > 0)) {
            aggResult.sValue = curSValue;
        } else if (curJValue != null && (aggResult.jValue == null || curJValue.compareTo(aggResult.jValue) > 0)) {
            aggResult.jValue = curJValue;
        }
    }

    private Boolean getBooleanValue(Row row) {
        if (aggregation == Aggregation.MIN || aggregation == Aggregation.MAX) {
            return row.getBoolean(BOOL_POS);
        } else {
            return null; //NOSONAR, null is used for further comparison
        }
    }

    private String getStringValue(Row row) {
        if (aggregation == Aggregation.MIN || aggregation == Aggregation.MAX) {
            return row.getString(STR_POS);
        } else {
            return null;
        }
    }

    private String getJsonValue(Row row) {
        if (aggregation == Aggregation.MIN || aggregation == Aggregation.MAX) {
            return row.getString(JSON_POS);
        } else {
            return null;
        }
    }

    private Long getLongValue(Row row) {
        if (aggregation == Aggregation.MIN || aggregation == Aggregation.MAX
                || aggregation == Aggregation.SUM || aggregation == Aggregation.AVG) {
            return row.getLong(LONG_POS);
        } else {
            return null;
        }
    }

    private Double getDoubleValue(Row row) {
        if (aggregation == Aggregation.MIN || aggregation == Aggregation.MAX
                || aggregation == Aggregation.SUM || aggregation == Aggregation.AVG) {
            return row.getDouble(DOUBLE_POS);
        } else {
            return null;
        }
    }

    private Optional<TsKvEntry> processAggregationResult(AggregationResult aggResult) {
        Optional<TsKvEntry> result;
        if (aggResult.dataType == null) {
            result = Optional.empty();
        } else if (aggregation == Aggregation.COUNT) {
            result = Optional.of(new BasicTsKvEntry(ts, new LongDataEntry(key, aggResult.count)));
        } else if (aggregation == Aggregation.AVG || aggregation == Aggregation.SUM) {
            result = processAvgOrSumResult(aggResult);
        } else if (aggregation == Aggregation.MIN || aggregation == Aggregation.MAX) {
            result = processMinOrMaxResult(aggResult);
        } else {
            result = Optional.empty();
        }
        if (!result.isPresent()) {
            log.trace("[{}][{}][{}] Aggregated data is empty.", key, ts, aggregation);
        }
        return result;
    }

    private Optional<TsKvEntry> processAvgOrSumResult(AggregationResult aggResult) {
        if (aggResult.count == 0 || (aggResult.dataType == DataType.DOUBLE && aggResult.dValue == null) || (aggResult.dataType == DataType.LONG && aggResult.lValue == null)) {
            return Optional.empty();
        } else if (aggResult.dataType == DataType.DOUBLE || aggResult.dataType == DataType.LONG) {
            if (aggregation == Aggregation.AVG || aggResult.hasDouble) {
                double sum = Optional.ofNullable(aggResult.dValue).orElse(0.0d) + Optional.ofNullable(aggResult.lValue).orElse(0L);
                return Optional.of(new BasicTsKvEntry(ts, new DoubleDataEntry(key, aggregation == Aggregation.SUM ? sum : (sum / aggResult.count))));
            } else {
                return Optional.of(new BasicTsKvEntry(ts, new LongDataEntry(key, aggregation == Aggregation.SUM ? aggResult.lValue : (aggResult.lValue / aggResult.count))));
            }
        }
        return Optional.empty();
    }

    private Optional<TsKvEntry> processMinOrMaxResult(AggregationResult aggResult) {
        if (aggResult.dataType == DataType.DOUBLE || aggResult.dataType == DataType.LONG) {
            if (aggResult.hasDouble) {
                double currentD = aggregation == Aggregation.MIN ? Optional.ofNullable(aggResult.dValue).orElse(Double.MAX_VALUE) : Optional.ofNullable(aggResult.dValue).orElse(Double.MIN_VALUE);
                double currentL = aggregation == Aggregation.MIN ? Optional.ofNullable(aggResult.lValue).orElse(Long.MAX_VALUE) : Optional.ofNullable(aggResult.lValue).orElse(Long.MIN_VALUE);
                return Optional.of(new BasicTsKvEntry(ts, new DoubleDataEntry(key, aggregation == Aggregation.MIN ? Math.min(currentD, currentL) : Math.max(currentD, currentL))));
            } else {
                return Optional.of(new BasicTsKvEntry(ts, new LongDataEntry(key, aggResult.lValue)));
            }
        } else if (aggResult.dataType == DataType.STRING) {
            return Optional.of(new BasicTsKvEntry(ts, new StringDataEntry(key, aggResult.sValue)));
        } else if (aggResult.dataType == DataType.JSON) {
            return Optional.of(new BasicTsKvEntry(ts, new JsonDataEntry(key, aggResult.jValue)));
        } else {
            return Optional.of(new BasicTsKvEntry(ts, new BooleanDataEntry(key, aggResult.bValue)));
        }
    }

    private class AggregationResult {
        DataType dataType = null;
        Boolean bValue = null;
        String sValue = null;
        String jValue = null;
        Double dValue = null;
        Long lValue = null;
        long count = 0;
        boolean hasDouble = false;
    }
}
