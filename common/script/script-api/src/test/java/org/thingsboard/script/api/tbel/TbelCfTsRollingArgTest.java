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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

public class TbelCfTsRollingArgTest {

    private final long ts = System.currentTimeMillis();

    private TbelCfTsRollingArg rollingArg;

    @BeforeEach
    void setUp() {
        rollingArg = new TbelCfTsRollingArg(
                new TbTimeWindow(ts - 30000, ts - 10),
                List.of(
                        new TbelCfTsDoubleVal(ts - 10, Double.NaN),
                        new TbelCfTsDoubleVal(ts - 20, 2.0),
                        new TbelCfTsDoubleVal(ts - 30, 8.0),
                        new TbelCfTsDoubleVal(ts - 40, Double.NaN),
                        new TbelCfTsDoubleVal(ts - 50, 3.0),
                        new TbelCfTsDoubleVal(ts - 60, 9.0),
                        new TbelCfTsDoubleVal(ts - 70, Double.NaN)
                )
        );
    }

    @Test
    void testMax() {
        assertThat(rollingArg.max()).isEqualTo(9.0);
        assertThat(rollingArg.max(false)).isNaN();
    }

    @Test
    void testMin() {
        assertThat(rollingArg.min()).isEqualTo(2.0);
        assertThat(rollingArg.min(false)).isNaN();
    }

    @Test
    void testMean() {
        assertThat(rollingArg.mean()).isEqualTo(5.5);
        assertThat(rollingArg.mean(false)).isNaN();
    }

    @Test
    void testStd() {
        assertThat(rollingArg.std()).isCloseTo(3.0413812651491097, within(0.001));
        assertThat(rollingArg.std(false)).isNaN();
    }

    @Test
    void testMedian() {
        assertThat(rollingArg.median()).isEqualTo(5.5);
        assertThat(rollingArg.median(false)).isNaN();
    }

    @Test
    void testCount() {
        assertThat(rollingArg.count()).isEqualTo(4);
        assertThat(rollingArg.count(false)).isEqualTo(7);
    }

    @Test
    void testLast() {
        assertThat(rollingArg.last()).isEqualTo(9.0);
        assertThat(rollingArg.last(false)).isNaN();
    }

    @Test
    void testFirst() {
        assertThat(rollingArg.first()).isEqualTo(2.0);
        assertThat(rollingArg.first(false)).isNaN();
    }

    @Test
    void testFirstAndLastWhenOnlyNaNAndIgnoreNaNIsFalse() {
        assertThat(rollingArg.first()).isEqualTo(2.0);
        rollingArg = new TbelCfTsRollingArg(
                new TbTimeWindow(ts - 30000, ts - 10),
                List.of(
                        new TbelCfTsDoubleVal(ts - 10, Double.NaN),
                        new TbelCfTsDoubleVal(ts - 40, Double.NaN),
                        new TbelCfTsDoubleVal(ts - 70, Double.NaN)
                )
        );
        assertThatThrownBy(rollingArg::first).isInstanceOf(IllegalArgumentException.class).hasMessage("Rolling argument values are empty.");
        assertThatThrownBy(rollingArg::last).isInstanceOf(IllegalArgumentException.class).hasMessage("Rolling argument values are empty.");
    }

    @Test
    void testSum() {
        assertThat(rollingArg.sum()).isEqualTo(22.0);
        assertThat(rollingArg.sum(false)).isNaN();
    }

    @Test
    void testEmptyValues() {
        rollingArg = new TbelCfTsRollingArg(new TbTimeWindow(0, 10), List.of());
        assertThatThrownBy(rollingArg::sum).isInstanceOf(IllegalArgumentException.class).hasMessage("Rolling argument values are empty.");
        assertThatThrownBy(rollingArg::max).isInstanceOf(IllegalArgumentException.class).hasMessage("Rolling argument values are empty.");
        assertThatThrownBy(rollingArg::min).isInstanceOf(IllegalArgumentException.class).hasMessage("Rolling argument values are empty.");
        assertThatThrownBy(rollingArg::mean).isInstanceOf(IllegalArgumentException.class).hasMessage("Rolling argument values are empty.");
        assertThatThrownBy(rollingArg::std).isInstanceOf(IllegalArgumentException.class).hasMessage("Rolling argument values are empty.");
        assertThatThrownBy(rollingArg::median).isInstanceOf(IllegalArgumentException.class).hasMessage("Rolling argument values are empty.");
        assertThatThrownBy(rollingArg::first).isInstanceOf(IllegalArgumentException.class).hasMessage("Rolling argument values are empty.");
        assertThatThrownBy(rollingArg::last).isInstanceOf(IllegalArgumentException.class).hasMessage("Rolling argument values are empty.");
    }

    @Test
    public void merge_two_rolling_args_ts_match_test() {
        TbTimeWindow tw = new TbTimeWindow(0, 60000);
        TbelCfTsRollingArg arg1 = new TbelCfTsRollingArg(tw, Arrays.asList(new TbelCfTsDoubleVal(1000, 1), new TbelCfTsDoubleVal(5000, 2), new TbelCfTsDoubleVal(15000, 3)));
        TbelCfTsRollingArg arg2 = new TbelCfTsRollingArg(tw, Arrays.asList(new TbelCfTsDoubleVal(1000, 11), new TbelCfTsDoubleVal(5000, 12), new TbelCfTsDoubleVal(15000, 13)));

        var result = arg1.merge(arg2);
        Assertions.assertEquals(3, result.getSize());
        Assertions.assertNotNull(result.getValues());
        Assertions.assertNotNull(result.getValues().get(0));
        Assertions.assertEquals(1000L, result.getValues().get(0).getTs());
        Assertions.assertEquals(1, result.getValues().get(0).getValues()[0]);
        Assertions.assertEquals(11, result.getValues().get(0).getValues()[1]);
    }

    @Test
    public void merge_two_rolling_args_with_timewindow_test() {
        TbTimeWindow tw = new TbTimeWindow(0, 60000);
        TbelCfTsRollingArg arg1 = new TbelCfTsRollingArg(tw, Arrays.asList(new TbelCfTsDoubleVal(1000, 1), new TbelCfTsDoubleVal(5000, 2), new TbelCfTsDoubleVal(15000, 3)));
        TbelCfTsRollingArg arg2 = new TbelCfTsRollingArg(tw, Arrays.asList(new TbelCfTsDoubleVal(1000, 11), new TbelCfTsDoubleVal(5000, 12), new TbelCfTsDoubleVal(15000, 13)));

        var result = arg1.merge(arg2, Collections.singletonMap("timeWindow", new TbTimeWindow(0, 10000)));
        Assertions.assertEquals(2, result.getSize());
        Assertions.assertNotNull(result.getValues());
        Assertions.assertNotNull(result.getValues().get(0));
        Assertions.assertEquals(1000L, result.getValues().get(0).getTs());
        Assertions.assertEquals(1, result.getValues().get(0).getValues()[0]);
        Assertions.assertEquals(11, result.getValues().get(0).getValues()[1]);

        result = arg1.merge(arg2, Collections.singletonMap("timeWindow", Map.of("startTs", 0L, "endTs", 10000)));
        Assertions.assertEquals(2, result.getSize());
        Assertions.assertNotNull(result.getValues());
        Assertions.assertNotNull(result.getValues().get(0));
        Assertions.assertEquals(1000L, result.getValues().get(0).getTs());
        Assertions.assertEquals(1, result.getValues().get(0).getValues()[0]);
        Assertions.assertEquals(11, result.getValues().get(0).getValues()[1]);
    }

    @Test
    public void merge_two_rolling_args_ts_mismatch_default_test() {
        TbTimeWindow tw = new TbTimeWindow(0, 60000);
        TbelCfTsRollingArg arg1 = new TbelCfTsRollingArg(tw, Arrays.asList(new TbelCfTsDoubleVal(100, 1), new TbelCfTsDoubleVal(5000, 2), new TbelCfTsDoubleVal(15000, 3)));
        TbelCfTsRollingArg arg2 = new TbelCfTsRollingArg(tw, Arrays.asList(new TbelCfTsDoubleVal(200, 11), new TbelCfTsDoubleVal(5000, 12), new TbelCfTsDoubleVal(15000, 13)));

        var result = arg1.merge(arg2);
        Assertions.assertEquals(3, result.getSize());
        Assertions.assertNotNull(result.getValues());

        TbelCfTsMultiDoubleVal item0 = result.getValues().get(0);
        Assertions.assertNotNull(item0);
        Assertions.assertEquals(200L, item0.getTs());
        Assertions.assertEquals(1, item0.getValues()[0]);
        Assertions.assertEquals(11, item0.getValues()[1]);
    }

    @Test
    public void merge_two_rolling_args_ts_mismatch_ignore_nan_disabled_test() {
        TbTimeWindow tw = new TbTimeWindow(0, 60000);
        TbelCfTsRollingArg arg1 = new TbelCfTsRollingArg(tw, Arrays.asList(new TbelCfTsDoubleVal(100, 1), new TbelCfTsDoubleVal(5000, 2), new TbelCfTsDoubleVal(15000, 3)));
        TbelCfTsRollingArg arg2 = new TbelCfTsRollingArg(tw, Arrays.asList(new TbelCfTsDoubleVal(200, 11), new TbelCfTsDoubleVal(5000, 12), new TbelCfTsDoubleVal(15000, 13)));

        var result = arg1.merge(arg2, Collections.singletonMap("ignoreNaN", false));
        Assertions.assertEquals(4, result.getSize());
        Assertions.assertNotNull(result.getValues());

        TbelCfTsMultiDoubleVal item0 = result.getValues().get(0);
        Assertions.assertNotNull(item0);
        Assertions.assertEquals(100L, item0.getTs());
        Assertions.assertEquals(1, item0.getValues()[0]);
        Assertions.assertEquals(Double.NaN, item0.getValues()[1]);

        TbelCfTsMultiDoubleVal item1 = result.getValues().get(1);
        Assertions.assertEquals(200L, item1.getTs());
        Assertions.assertEquals(1, item1.getValues()[0]);
        Assertions.assertEquals(11, item1.getValues()[1]);
    }

}