// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.kv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.ZoneId;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BaseReadTsKvQuery extends BaseTsKvQuery implements ReadTsKvQuery {

    private final AggregationParams aggParameters;
    private final int limit;
    private final String order;

    public BaseReadTsKvQuery(String key, long startTs, long endTs, long interval, int limit, Aggregation aggregation) {
        this(key, startTs, endTs, interval, limit, aggregation, "DESC");
    }

    public BaseReadTsKvQuery(String key, long startTs, long endTs, long interval, int limit, Aggregation aggregation, String descOrder) {
        this(key, startTs, endTs, AggregationParams.of(aggregation, IntervalType.MILLISECONDS, ZoneId.systemDefault(), interval), limit, descOrder);
    }

    public BaseReadTsKvQuery(String key, long startTs, long endTs, AggregationParams parameters, int limit) {
        this(key, startTs, endTs, parameters, limit, "DESC");
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public BaseReadTsKvQuery(
            @JsonProperty(value = "key")  String key,
            @JsonProperty(value = "startTs") long startTs,
            @JsonProperty(value = "endTs") long endTs,
            @JsonProperty(value = "aggParameters") AggregationParams parameters,
            @JsonProperty(value = "limit") int limit,
            @JsonProperty(value = "order") String order
    ) {
        super(key, startTs, endTs);
        this.aggParameters = parameters;
        this.limit = limit;
        this.order = order;
    }

    public BaseReadTsKvQuery(String key, long startTs, long endTs) {
        this(key, startTs, endTs, AggregationParams.milliseconds(Aggregation.AVG, endTs - startTs), 1, "DESC");
    }

    public BaseReadTsKvQuery(String key, long startTs, long endTs, int limit, String order) {
        this(key, startTs, endTs, AggregationParams.none(), limit, order);
    }

    public BaseReadTsKvQuery(ReadTsKvQuery query, long startTs, long endTs) {
        super(query.getId(), query.getKey(), startTs, endTs);
        this.aggParameters = query.getAggParameters();
        this.limit = query.getLimit();
        this.order = query.getOrder();
    }
}
