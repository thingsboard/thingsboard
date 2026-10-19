// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.aggregation.function;

import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CountUniqueAggEntry implements AggEntry {

    private final Set<String> items = new HashSet<>();

    @Override
    public void update(Object value) {
        if (value != null) {
            items.add(String.valueOf(value));
        }
    }

    @Override
    public Optional<Object> result(Integer precision) {
        return Optional.of(items.size());
    }

    @Override
    public AggFunction getType() {
        return AggFunction.COUNT_UNIQUE;
    }
}
