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

import org.thingsboard.script.api.tbel.TbUtils;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AvgAggEntry extends BaseAggEntry {

    private BigDecimal sum = BigDecimal.ZERO;
    private long count = 0L;

    @Override
    protected void doUpdate(double value) {
        if (value != 0.0) {
            sum = sum.add(BigDecimal.valueOf(value));
        }
        this.count++;
    }

    @Override
    protected Object prepareResult(Integer precision) {
        double result = sum.divide(BigDecimal.valueOf(count), RoundingMode.HALF_UP).doubleValue();
        return TbUtils.roundResult(result, precision);
    }

    @Override
    public AggFunction getType() {
        return AggFunction.AVG;
    }
}
