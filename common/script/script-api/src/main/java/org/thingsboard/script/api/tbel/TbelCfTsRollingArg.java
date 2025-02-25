/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static org.thingsboard.script.api.tbel.TbelCfTsDoubleVal.OBJ_SIZE;

public class TbelCfTsRollingArg implements TbelCfArg, Iterable<TbelCfTsDoubleVal> {

    @Getter
    private final TbTimeWindow timeWindow;
    @Getter
    private final List<TbelCfTsDoubleVal> values;

    @JsonCreator
    public TbelCfTsRollingArg(
            @JsonProperty("timeWindow") TbTimeWindow timeWindow,
            @JsonProperty("values") List<TbelCfTsDoubleVal> values
    ) {
        this.timeWindow = timeWindow;
        this.values = Collections.unmodifiableList(values);
    }

    public TbelCfTsRollingArg(int limit, long timeWindow, List<TbelCfTsDoubleVal> values) {
        long ts = System.currentTimeMillis();
        this.timeWindow = new TbTimeWindow(ts - timeWindow, ts, limit);
        this.values = Collections.unmodifiableList(values);
    }

    @Override
    public long memorySize() {
        return 12 + values.size() * OBJ_SIZE;
    }

    @JsonIgnore
    public List<TbelCfTsDoubleVal> getValue() {
        return values;
    }

    public double max() {
        if (values.isEmpty()) {
            return 0;
        }

        double max = Double.MIN_VALUE;
        for (TbelCfTsDoubleVal value : values) {
            double val = value.getValue();
            if (Double.isNaN(val)) {
                return val;
            }
            if (max < val) {
                max = val;
            }
        }
        return max;
    }

    public double min() {
        if (values.isEmpty()) {
            return 0;
        }

        double min = Double.MAX_VALUE;
        for (TbelCfTsDoubleVal value : values) {
            double val = value.getValue();
            if (Double.isNaN(val)) {
                return Double.NaN;
            }
            if (min > val) {
                min = val;
            }
        }
        return min;
    }

    public double mean() {
        if (values.isEmpty()) {
            return 0;
        }

        double sum = sum();
        return Double.isNaN(sum) ? Double.NaN : sum / values.size();
    }

    public double std() {
        if (values.isEmpty()) {
            return 0;
        }

        double mean = mean();
        if (Double.isNaN(mean)) {
            return Double.NaN;
        }

        double sum = 0;
        for (TbelCfTsDoubleVal value : values) {
            double val = value.getValue();
            if (Double.isNaN(val)) {
                return Double.NaN;
            }
            sum += Math.pow(val - mean, 2);
        }
        return Math.sqrt(sum / values.size());
    }

    public double median() {
        if (values.isEmpty()) {
            return 0;
        }

        List<Double> sortedValues = new ArrayList<>();
        for (TbelCfTsDoubleVal value : values) {
            double val = value.getValue();
            if (Double.isNaN(val)) {
                return Double.NaN;
            }
            sortedValues.add(val);
        }
        Collections.sort(sortedValues);

        int size = sortedValues.size();
        return (size % 2 == 1)
                ? sortedValues.get(size / 2)
                : (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
    }

    public double count() {
        return hasNaN() ? Double.NaN : values.size();
    }

    public double last() {
        if (values.isEmpty()) {
            return 0;
        }

        return hasNaN() ? Double.NaN : values.get(values.size() - 1).getValue();
    }

    public double first() {
        if (values.isEmpty()) {
            return 0;
        }

        return hasNaN() ? Double.NaN : values.get(0).getValue();
    }

    public double sum() {
        if (values.isEmpty()) {
            return 0;
        }

        double sum = 0;
        for (TbelCfTsDoubleVal value : values) {
            double val = value.getValue();
            if (Double.isNaN(val)) {
                return Double.NaN;
            }
            sum += val;
        }
        return sum;
    }

    private boolean hasNaN() {
        for (TbelCfTsDoubleVal value : values) {
            if (Double.isNaN(value.getValue())) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public int getSize() {
        return values.size();
    }

    @Override
    public Iterator<TbelCfTsDoubleVal> iterator() {
        return values.iterator();
    }

    @Override
    public void forEach(Consumer<? super TbelCfTsDoubleVal> action) {
        values.forEach(action);
    }

    @Override
    public String getType() {
        return "TS_ROLLING";
    }

}
