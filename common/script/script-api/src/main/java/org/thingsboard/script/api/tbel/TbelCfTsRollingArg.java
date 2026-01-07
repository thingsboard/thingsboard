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
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.thingsboard.common.util.JacksonUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
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

    public TbelCfTsRollingArg(long timeWindow, List<TbelCfTsDoubleVal> values) {
        long ts = System.currentTimeMillis();
        this.timeWindow = new TbTimeWindow(ts - timeWindow, ts);
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

    public double avg() {
        return avg(true);
    }

    public double avg(boolean ignoreNaN) {
        return mean(ignoreNaN);
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

    public TbelCfTsRollingData merge(TbelCfTsRollingArg other) {
        return mergeAll(Collections.singletonList(other), null);
    }

    public TbelCfTsRollingData merge(TbelCfTsRollingArg other, Map<String, Object> settings) {
        return mergeAll(Collections.singletonList(other), settings);
    }

    public TbelCfTsRollingData mergeAll(List<TbelCfTsRollingArg> others) {
        return mergeAll(others, null);
    }

    public TbelCfTsRollingData mergeAll(List<TbelCfTsRollingArg> others, Map<String, Object> settings) {
        List<TbelCfTsRollingArg> args = new ArrayList<>(others.size() + 1);
        args.add(this);
        args.addAll(others);

        boolean ignoreNaN = true;
        if (settings != null && settings.containsKey("ignoreNaN")) {
            ignoreNaN = Boolean.parseBoolean(settings.get("ignoreNaN").toString());
        }

        TbTimeWindow timeWindow = null;
        if (settings != null && settings.containsKey("timeWindow")) {
            var twVar = settings.get("timeWindow");
            if (twVar instanceof TbTimeWindow) {
                timeWindow = (TbTimeWindow) settings.get("timeWindow");
            } else if (twVar instanceof Map twMap) {
                timeWindow = new TbTimeWindow(Long.valueOf(twMap.get("startTs").toString()), Long.valueOf(twMap.get("endTs").toString()));
            } else {
                timeWindow = JacksonUtil.fromString(settings.get("timeWindow").toString(), TbTimeWindow.class);
            }
        }

        TreeSet<Long> allTimestamps = new TreeSet<>();
        long startTs = Long.MAX_VALUE;
        long endTs = Long.MIN_VALUE;
        for (TbelCfTsRollingArg arg : args) {
            for (TbelCfTsDoubleVal val : arg.getValues()) {
                allTimestamps.add(val.getTs());
            }
            startTs = Math.min(startTs, arg.getTimeWindow().getStartTs());
            endTs = Math.max(endTs, arg.getTimeWindow().getEndTs());
        }

        List<TbelCfTsMultiDoubleVal> data = new ArrayList<>();

        int[] lastIndex = new int[args.size()];
        double[] result = new double[args.size()];
        Arrays.fill(result, Double.NaN);

        for (long ts : allTimestamps) {
            for (int i = 0; i < args.size(); i++) {
                var arg = args.get(i);
                var values = arg.getValues();
                while (lastIndex[i] < values.size() && values.get(lastIndex[i]).getTs() <= ts) {
                    result[i] = values.get(lastIndex[i]).getValue();
                    lastIndex[i]++;
                }
            }
            if (timeWindow == null || timeWindow.matches(ts)) {
                if (ignoreNaN) {
                    boolean skip = false;
                    for (int i = 0; i < args.size(); i++) {
                        if (Double.isNaN(result[i])) {
                            skip = true;
                            break;
                        }
                    }
                    if (!skip) {
                        data.add(new TbelCfTsMultiDoubleVal(ts, Arrays.copyOf(result, result.length)));
                    }
                } else {
                    data.add(new TbelCfTsMultiDoubleVal(ts, Arrays.copyOf(result, result.length)));
                }
            }
        }

        return new TbelCfTsRollingData(timeWindow != null ? timeWindow : new TbTimeWindow(startTs, endTs), data);
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
