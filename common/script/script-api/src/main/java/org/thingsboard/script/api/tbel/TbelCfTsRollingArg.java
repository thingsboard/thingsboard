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
        return max(true);
    }

    public double max(boolean ignoreNaN) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Rolling argument values are empty.");
        }

        double max = Double.MIN_VALUE;
        for (TbelCfTsDoubleVal value : values) {
            double val = value.getValue();
            if (!ignoreNaN && Double.isNaN(val)) {
                return val;
            }
            if (max < val) {
                max = val;
            }
        }
        return max;
    }

    public double min() {
        return min(true);
    }

    public double min(boolean ignoreNaN) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Rolling argument values are empty.");
        }

        double min = Double.MAX_VALUE;
        for (TbelCfTsDoubleVal value : values) {
            double val = value.getValue();
            if (!ignoreNaN && Double.isNaN(val)) {
                return Double.NaN;
            }
            if (min > val) {
                min = val;
            }
        }
        return min;
    }

    public double mean() {
        return mean(true);
    }

    public double mean(boolean ignoreNaN) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Rolling argument values are empty.");
        }

        return sum(ignoreNaN) / count(ignoreNaN);
    }

    public double std() {
        return std(true);
    }

    public double std(boolean ignoreNaN) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Rolling argument values are empty.");
        }

        double mean = mean(ignoreNaN);
        if (!ignoreNaN && Double.isNaN(mean)) {
            return Double.NaN;
        }

        double sum = 0;
        for (TbelCfTsDoubleVal value : values) {
            double val = value.getValue();
            if (Double.isNaN(val)) {
                if (!ignoreNaN) {
                    return Double.NaN;
                }
            } else {
                sum += Math.pow(val - mean, 2);
            }
        }
        return Math.sqrt(sum / count(ignoreNaN));
    }

    public double median() {
        return median(true);
    }

    public double median(boolean ignoreNaN) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Rolling argument values are empty.");
        }

        List<Double> sortedValues = new ArrayList<>();
        for (TbelCfTsDoubleVal value : values) {
            double val = value.getValue();
            if (Double.isNaN(val)) {
                if (!ignoreNaN) {
                    return Double.NaN;
                }
            } else {
                sortedValues.add(val);
            }
        }
        Collections.sort(sortedValues);

        int size = sortedValues.size();
        return (size % 2 == 1)
                ? sortedValues.get(size / 2)
                : (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
    }

    public int count() {
        return count(true);
    }

    public int count(boolean ignoreNaN) {
        int count = 0;
        if (ignoreNaN) {
            for (TbelCfTsDoubleVal value : values) {
                if (!Double.isNaN(value.getValue())) {
                    count++;
                }
            }
            return count;
        }
        return values.size();
    }

    public double last() {
        return last(true);
    }

    public double last(boolean ignoreNaN) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Rolling argument values are empty.");
        }

        double value = values.get(values.size() - 1).getValue();
        if (!Double.isNaN(value) || !ignoreNaN) {
            return value;
        }
        for (int i = values.size() - 2; i >= 0; i--) {
            double prevValue = values.get(i).getValue();
            if (!Double.isNaN(prevValue)) {
                return prevValue;
            }
        }
        throw new IllegalArgumentException("Rolling argument values are empty.");
    }

    public double first() {
        return first(true);
    }

    public double first(boolean ignoreNaN) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Rolling argument values are empty.");
        }

        double firstValue = values.get(0).getValue();
        if (!Double.isNaN(firstValue) || !ignoreNaN) {
            return firstValue;
        }
        for (int i = 1; i < values.size(); i++) {
            double nextValue = values.get(i).getValue();
            if (!Double.isNaN(nextValue)) {
                return nextValue;
            }
        }
        throw new IllegalArgumentException("Rolling argument values are empty.");
    }

    public double sum() {
        return sum(true);
    }

    public double sum(boolean ignoreNaN) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Rolling argument values are empty.");
        }

        double sum = 0;
        for (TbelCfTsDoubleVal value : values) {
            double val = value.getValue();
            if (Double.isNaN(val)) {
                if (!ignoreNaN) {
                    return Double.NaN;
                }
            } else {
                sum += val;
            }
        }
        return sum;
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
    public String getType() {
        return "TS_ROLLING";
    }

}
