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
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AggIntervalTest {

    private static final String TZ = "Europe/Kiev";

    @ParameterizedTest
    @MethodSource("intervals")
    void testGetStartAndEndWithoutOffset(LongFunction<AggInterval> intervalCreator) {
        AggInterval interval = intervalCreator.apply(0L);

        ZonedDateTime dateTime = ZonedDateTime.of(
                // 2025.11.11 00:00:00
                2025, 11, 11, 0, 0, 0, 0, ZoneId.of(TZ)
        );
        long startTs = interval.getDateTimeIntervalStartTs(dateTime);
        long endTs = interval.getDateTimeIntervalEndTs(dateTime);

        assertThat(endTs).isGreaterThan(startTs);
        assertThat(endTs - startTs).isEqualTo(interval.getCurrentIntervalDurationMillis());
    }

    @ParameterizedTest
    @MethodSource("intervals")
    void testApplyOffset(LongFunction<AggInterval> intervalCreator) {
        long offsetSec = TimeUnit.MINUTES.toSeconds(15);
        AggInterval intervalWithOffset = intervalCreator.apply(offsetSec);
        AggInterval intervalNoOffset = intervalCreator.apply(0L);

        ZonedDateTime dateTime = ZonedDateTime.of(
                // 2025.11.11 11:20:00 - chosen so 15m offset shifts into a new interval
                2025, 11, 11, 11, 20, 0, 0, ZoneId.of(TZ)
        );

        long startWithOffsetTs = intervalWithOffset.getDateTimeIntervalStartTs(dateTime);
        long startNoOffsetTs = intervalNoOffset.getDateTimeIntervalStartTs(dateTime);

        ZonedDateTime startWithOffset = Instant.ofEpochMilli(startWithOffsetTs).atZone(intervalWithOffset.getZoneId());
        ZonedDateTime startNoOffset = Instant.ofEpochMilli(startNoOffsetTs).atZone(intervalNoOffset.getZoneId());

        long actualOffset = Duration.between(startNoOffset, startWithOffset).toSeconds();
        assertThat(actualOffset).isEqualTo(offsetSec);
    }

    private static Stream<Arguments> intervals() {
        return Stream.of(
                Arguments.of((LongFunction<AggInterval>) offset -> new HourInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new DayInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new WeekInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new WeekSunSatInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new MonthInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new QuarterInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new YearInterval(TZ, offset)),
                Arguments.of((LongFunction<AggInterval>) offset -> new CustomInterval(TZ, offset, TimeUnit.HOURS.toSeconds(4)))
        );
    }

    @ParameterizedTest
    @MethodSource("nextIntervalFromExactDate")
    void testNextIntervalFromExactDate(LongFunction<AggInterval> intervalCreator, Function<ZonedDateTime, ZonedDateTime> expectedDateTimeFunction) {
        AggInterval interval = intervalCreator.apply(0L);

        ZonedDateTime currentStart = ZonedDateTime.of(
                2025, 11, 11, 0, 0, 0, 0, ZoneId.of(TZ)
        );

        ZonedDateTime nextStart = interval.getNextIntervalStart(currentStart);

        assertThat(nextStart).isEqualTo(expectedDateTimeFunction.apply(currentStart));
    }

    private static Stream<Arguments> nextIntervalFromExactDate() {
        return Stream.of(
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new HourInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusHours(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new DayInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusDays(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new WeekInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusWeeks(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new WeekSunSatInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusWeeks(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new MonthInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusMonths(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new QuarterInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusMonths(3)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new YearInterval(TZ, offset),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusYears(1)
                ),
                Arguments.of(
                        (LongFunction<AggInterval>) offset -> new CustomInterval(TZ, offset, TimeUnit.HOURS.toSeconds(4)),
                        (Function<ZonedDateTime, ZonedDateTime>) currentInterval -> currentInterval.plusHours(4)
                )
        );
    }

}
