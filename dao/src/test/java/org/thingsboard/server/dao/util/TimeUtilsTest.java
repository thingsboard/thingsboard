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
package org.thingsboard.server.dao.util;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.kv.IntervalType;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilsTest {

    @Test
    void testWeekEnd() {
        long ts = 1704899727000L; // Wednesday, January 10 15:15:27 GMT
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.WEEK, ZoneId.of("Europe/Kyiv"))).isEqualTo(1705183200000L); // Sunday, January 14, 2024 0:00:00 GMT+02:00
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.WEEK_ISO, ZoneId.of("Europe/Kyiv"))).isEqualTo(1705269600000L); // Monday, January 15, 2024 0:00:00 GMT+02:00

        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.WEEK, ZoneId.of("Europe/Amsterdam"))).isEqualTo(1705186800000L); // Sunday, January 14, 2024 0:00:00 GMT+01:00
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.WEEK_ISO, ZoneId.of("Europe/Amsterdam"))).isEqualTo(1705273200000L); // Monday, January 15, 2024 0:00:00 GMT+01:00

        ts = 1704621600000L; // Sunday, January 7, 2024 12:00:00 GMT+02:00
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.WEEK, ZoneId.of("Europe/Kyiv"))).isEqualTo(1705183200000L); // Sunday, January 14, 2024 0:00:00 GMT+02:00
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.WEEK_ISO, ZoneId.of("Europe/Kyiv"))).isEqualTo(1704664800000L); // Monday, January 8, 2024 0:00:00 GMT+02:00
    }


    @Test
    void testMonthEnd() {
        long ts = 1704899727000L; // Wednesday, January 10 15:15:27 GMT
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.MONTH, ZoneId.of("Europe/Kyiv"))).isEqualTo(1706738400000L); // Thursday, February 1, 2024 0:00:00 GMT+02:00
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.MONTH, ZoneId.of("Europe/Amsterdam"))).isEqualTo(1706742000000L); // Monday, January 15, 2024 0:00:00 GMT+02:00
    }

    @Test
    void testQuarterEnd() {
        long ts = 1704899727000L; // Wednesday, January 10 15:15:27 GMT
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.QUARTER, ZoneId.of("Europe/Kyiv"))).isEqualTo(1711918800000L); // Monday, April 1, 2024 0:00:00 GMT+03:00 DST
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.QUARTER, ZoneId.of("Europe/Amsterdam"))).isEqualTo(1711922400000L); // Monday, April 1, 2024 1:00:00 GMT+03:00 DST

        ts = 1711929600000L; // Monday, April 1, 2024 3:00:00 GMT+03:00
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.QUARTER, ZoneId.of("Europe/Kyiv"))).isEqualTo(1719781200000L); // Monday, July 1, 2024 0:00:00 GMT+03:00 DST
        assertThat(TimeUtils.calculateIntervalEnd(ts, IntervalType.QUARTER, ZoneId.of("America/New_York"))).isEqualTo(1711944000000L); // Monday, April 1, 2024 7:00:00 GMT+03:00 DST
    }

}
