/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import java.util.Optional;

public abstract class BaseAggEntry implements AggEntry {

    private boolean hasResult = false;

    @Override
    public void update(Object value) {
        doUpdate(extractDoubleValue(value));
        hasResult = true;
    }

    @Override
    public Optional<Object> result(Integer precision) {
        if (hasResult) {
            hasResult = false;
            return Optional.of(prepareResult(precision));
        } else {
            return Optional.empty();
        }
    }

    protected abstract void doUpdate(double value);

    protected abstract Object prepareResult(Integer precision);

    protected double extractDoubleValue(Object value) {
        try {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            throw new NumberFormatException("Cannot parse value " + value.toString());
        }
    }

}
