// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Collections;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;

public class WeeklyRepeatTest {

    public static final String TIMEZONE = "EET";

    @Test
    public void getNextTest() throws ParseException {
        long startTime = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-09-10T01:22:00+03:00Z").getTime();
        long ts = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-11-24T01:00:00+02:00Z").getTime();
        long nextTs = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-04-03T01:00:00+03:00Z").getTime();
        long endTs = ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-11-24T01:00:00+02:00Z").getTime();

        WeeklyRepeat weeklyRepeat = new WeeklyRepeat();
        weeklyRepeat.setRepeatOn(Collections.singletonList(4));
        weeklyRepeat.setEndsOn(endTs);

        assertThat(weeklyRepeat.getNext(startTime, ts, TIMEZONE)).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2022-11-24T01:22:00+02:00Z").getTime());

        assertThat(weeklyRepeat.getNext(startTime, nextTs, TIMEZONE)).isEqualTo(
                ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse("2023-04-06T01:22:00+03:00Z").getTime());
    }
}