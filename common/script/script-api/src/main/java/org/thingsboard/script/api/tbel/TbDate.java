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

import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.function.BiFunction;

public class TbDate extends Date {

    private static final DateTimeFormatter isoDateFormatter = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd[[ ]['T']HH:mm[:ss[.SSS]][ ][XXX][Z][z][VV][O]]").withZone(ZoneId.systemDefault());

    private static final ThreadLocal<DateFormat> isoDateFormat = ThreadLocal.withInitial(() ->
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

    public TbDate() {
        super();
    }

    public TbDate(String s) {
        super(parse(s));
    }

    public TbDate(long date) {
        super(date);
    }

    public TbDate(int year, int month, int date) {
        this(year, month, date, 0, 0, 0);
    }

    public TbDate(int year, int month, int date, int hrs, int min) {
        this(year, month, date, hrs, min, 0);
    }

    public TbDate(int year, int month, int date,
                  int hrs, int min, int second) {
        super(new GregorianCalendar(year, month, date, hrs, min, second).getTimeInMillis());
    }

    public String toDateString() {
        DateFormat formatter = DateFormat.getDateInstance();
        return formatter.format(this);
    }

    public String toTimeString() {
        DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
        return formatter.format(this);
    }

    public String toISOString() {
        return isoDateFormat.get().format(this);
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

    @Override
    public String toLocaleString() {
        return toLocaleString(null, null);
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
        ZonedDateTime zdt = this.toInstant().atZone(options.getTimeZone().toZoneId());
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

    public static long parse(String value, String format) {
        try {
            DateFormat dateFormat = new SimpleDateFormat(format);
            return dateFormat.parse(value).getTime();
        } catch (Exception e) {
            return -1;
        }
    }

    public static long parse(String value) {
        try {
            TemporalAccessor accessor = isoDateFormatter.parseBest(value,
                    ZonedDateTime::from,
                    LocalDateTime::from,
                    LocalDate::from);
            Instant instant = Instant.from(accessor);
            return Instant.EPOCH.until(instant, ChronoUnit.MILLIS);
        } catch (Exception e) {
            try {
                return Date.parse(value);
            } catch (IllegalArgumentException e2) {
                return -1;
            }
        }
    }

    public static long UTC(int year, int month, int date,
                           int hrs, int min, int sec) {
        return Date.UTC(year - 1900, month, date, hrs, min, sec);
    }

}
