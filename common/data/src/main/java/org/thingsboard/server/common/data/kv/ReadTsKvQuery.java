// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

public interface ReadTsKvQuery extends TsKvQuery {

    AggregationParams getAggParameters();

    default long getInterval(){
        return getAggParameters().getInterval();
    }

    default Aggregation getAggregation() {
        return getAggParameters().getAggregation();
    }

    int getLimit();

    String getOrder();

}
