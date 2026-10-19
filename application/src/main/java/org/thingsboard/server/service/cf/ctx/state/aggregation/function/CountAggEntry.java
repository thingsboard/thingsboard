// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.aggregation.function;

import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;

import java.util.Optional;

public class CountAggEntry implements AggEntry {

    private long count = 0L;

    @Override
    public void update(Object value) {
        count++;
    }

    @Override
    public Optional<Object> result(Integer precision) {
        return Optional.of(count);
    }

    @Override
    public AggFunction getType() {
        return AggFunction.COUNT;
    }

}
