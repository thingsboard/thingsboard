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
package org.thingsboard.server.service.solutions.data.values;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

public class GeneratorTools {

    private static final Random random = new Random();

    public static long randomLong(double min, double max) {
        return (long) randomDouble(min, max);
    }

    public static double randomDouble(double min, double max) {
        return min + random.nextDouble() * Math.abs(max - min);
    }

    public static double getMultiplier(long ts, double holidayMultiplier, double workHoursMultiplier, double nightHoursMultiplier) {
        Date date = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        double multiplier = 1.0;
        if (dayOfWeek == 1 || dayOfWeek == 7) {
            multiplier *= holidayMultiplier;
        }

        if (hour > 8 && hour < 18) {
            multiplier *= workHoursMultiplier;
        } else if (hour < 6 || hour > 22) {
            multiplier *= nightHoursMultiplier;
        }
        return multiplier;
    }

    public static int getHour(TimeZone tz, long ts) {
        Date date = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTimeZone(tz);
        c.setTime(date);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    public static int getMinute(TimeZone tz, long ts) {
        Date date = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTimeZone(tz);
        c.setTime(date);
        return c.get(Calendar.MINUTE);
    }

    public static boolean isHoliday(long ts) {
        Date date = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek == 1 || dayOfWeek == 7;
    }

    public static boolean isWorkHour(long ts) {
        Date date = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int hour = c.get(Calendar.HOUR_OF_DAY);

        return hour > 8 && hour < 18;
    }

    public static boolean isNightHour(long ts) {
        Date date = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int hour = c.get(Calendar.HOUR_OF_DAY);

        return hour < 6 || hour >= 22;
    }

}
