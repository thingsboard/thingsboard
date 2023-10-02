/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import org.mvel2.ConversionException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;

import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.function.BiFunction;

public class TbDate  implements InstantSource {

    private static Instant instant;

    private static String patternDefault = "%s-%s-%sT%s:%s:%s.%d%s";

    public TbDate() {
        instant = Instant.now();
    }

    public TbDate(String s) {
        try{
            if (s.length() > 0 && Character.isDigit(s.charAt(0))) {
                // assuming UTC instant a la "2007-12-03T10:15:30.00Z"
                instant = Instant.parse(s);
            }
            else {
                // assuming RFC-1123 value a la "Tue, 3 Jun 2008 11:05:30 GMT"
                instant = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(s));
            }
        } catch (final DateTimeParseException ex) {
            final ConversionException exception = new ConversionException("Cannot parse value [" + s + "] as instant", ex);
            throw exception;
        }
    }

    public TbDate(long dateMilliSecond) {
        instant = Instant.ofEpochMilli(dateMilliSecond);
    }

    public TbDate(int year, int month, int date, String... tz) {
        this(year, month, date, 0, 0, 0, 0, tz);
    }

    public TbDate(int year, int month, int date, int hrs, int min, String... tz) {
        this(year, month, date, hrs, min, 0, 0, tz);
    }

    public TbDate(int year, int month, int date, int hrs, int min, int second, String... tz) {
        this(year, month, date, hrs, min, second, 0, tz);
    }

    public TbDate(int year, int month, int date, int hrs, int min, int second, int secondMilli, String... tz) {
        this(createDateTimeFromPattern (year, month, date, hrs, min, second, secondMilli, tz));
    }

    @Override
    public Instant instant() {
        return instant;
    }

    public String toDateString() {
        return toLocaleDateString();
    }

    public String toTimeString() {
        return toLocaleTimeString();
    }

    public String toISOString() {
        return instant.toString();
    }

    public String toLocaleDateString() {
        return toLocaleDateString(null, null);
    }

    public String toLocaleDateString(String locale) {
        return toLocaleDateString(locale, null);
    }

    public String toLocaleDateString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> DateTimeFormatter.ofLocalizedDate(options.getDateStyle()).withLocale(locale));
    }

    public String toLocaleTimeString() {
        return toLocaleTimeString(null, null);
    }

    public String toLocaleTimeString(String locale) {
        return toLocaleTimeString(locale, null);
    }

    public String toLocaleTimeString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> DateTimeFormatter.ofLocalizedTime(options.getTimeStyle()).withLocale(locale));
    }

    public String toLocaleString() {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return localDateTime.toString();
    }

    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public String toLocaleString(String locale) {
        return toLocaleString(locale, null);
    }

    public String toLocaleString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> {
            String formatPattern =
                    DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                            options.getDateStyle(),
                            options.getTimeStyle(),
                            IsoChronology.INSTANCE,
                            locale);
            return DateTimeFormatter.ofPattern(formatPattern, locale);
        });
    }

    public String toLocaleString(String localeStr, String optionsStr, BiFunction<Locale, DateTimeFormatOptions, DateTimeFormatter> formatterBuilder) {
        Locale locale = StringUtils.isNotEmpty(localeStr) ? Locale.forLanguageTag(localeStr) : Locale.getDefault();
        DateTimeFormatOptions options = getDateFormattingOptions(optionsStr);
        ZonedDateTime zdt = this.instant().atZone(options.getTimeZone().toZoneId());
        DateTimeFormatter formatter;
        if (StringUtils.isNotEmpty(options.getPattern())) {
            formatter = new DateTimeFormatterBuilder().appendPattern(options.getPattern()).toFormatter(locale);
        } else {
            formatter = formatterBuilder.apply(locale, options);
        }
        return formatter.format(zdt);
    }

    private static DateTimeFormatOptions getDateFormattingOptions(String options) {
        DateTimeFormatOptions opt = null;
        if (StringUtils.isNotEmpty(options)) {
            try {
                opt = JacksonUtil.fromString(options, DateTimeFormatOptions.class);
            } catch (IllegalArgumentException iae) {
                opt = new DateTimeFormatOptions(options);
            }
        }
        if (opt == null) {
            opt = new DateTimeFormatOptions();
        }
        return opt;
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static long parseSecond() {
        return instant.getEpochSecond();
    }

    public static long parseSecondMilli() {
        return instant.toEpochMilli();
    }

    public static long UTC(int year, int month, int date,
                           int hrs, int min, int sec) {
        return Date.UTC(year - 1900, month, date, hrs, min, sec);
    }

    private static String createDateTimeFromPattern (int year, int month, int date, int hrs, int min, int second, int secondMilli, String... tz) {
        String yearStr = String.format("%04d", year);
        if (yearStr.substring(0, 2).equals("00"))  yearStr = "20" + yearStr.substring(2,4);
        String monthStr = String.format("%02d", month);
        String dateStr = String.format("%02d", date);
        String hrsStr = String.format("%02d", hrs);
        String minStr = String.format("%02d", min);
        String secondStr = String.format("%02d", second);
        String tzStr = tz.length > 0 ? Arrays.stream(tz).findFirst().get() : "Z";
        return String.format(patternDefault, yearStr, monthStr, dateStr, hrsStr, minStr, secondStr, secondMilli, tzStr);
    }
}
