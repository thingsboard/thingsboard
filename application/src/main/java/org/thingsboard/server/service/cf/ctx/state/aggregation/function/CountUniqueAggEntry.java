/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.cf.ctx.state.aggregation.function;

import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;

import java.util.Optional;
import java.util.Set;

public class CountUniqueAggEntry implements AggEntry {

    private Set<String> items;

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
