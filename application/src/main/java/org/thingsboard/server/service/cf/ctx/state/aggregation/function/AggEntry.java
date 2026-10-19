// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.aggregation.function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;

import java.util.Optional;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AvgAggEntry.class, name = "AVG"),
        @JsonSubTypes.Type(value = CountAggEntry.class, name = "COUNT"),
        @JsonSubTypes.Type(value = CountUniqueAggEntry.class, name = "COUNT_UNIQUE"),
        @JsonSubTypes.Type(value = MaxAggEntry.class, name = "MAX"),
        @JsonSubTypes.Type(value = MinAggEntry.class, name = "MIN"),
        @JsonSubTypes.Type(value = SumAggEntry.class, name = "SUM")
})
public interface AggEntry {

    @JsonIgnore
    AggFunction getType();

    void update(Object value);

    Optional<Object> result(Integer precision);

    static AggEntry createAggFunction(AggFunction function) {
        return switch (function) {
            case MIN -> new MinAggEntry();
            case MAX -> new MaxAggEntry();
            case SUM -> new SumAggEntry();
            case AVG -> new AvgAggEntry();
            case COUNT -> new CountAggEntry();
            case COUNT_UNIQUE -> new CountUniqueAggEntry();
        };
    }

}
