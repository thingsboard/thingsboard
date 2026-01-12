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

import com.fasterxml.jackson.annotation.JsonValue;
import org.mvel2.ConversionException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TbDate implements Serializable, Cloneable {

    private Instant instant;

    private static final ZoneId zoneIdUTC = ZoneId.of("UTC");
    private static final Locale localeUTC = Locale.forLanguageTag("UTC");

    private static final DateTimeFormatter isoDateFormatter = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd[[ ]['T']HH:mm[:ss[.SSS]][ ][XXX][Z][z][VV][O]]").withZone(ZoneId.systemDefault());

    public TbDate() {
        this.instant = Instant.now();
    }

    public TbDate(String s) {
        this.instant = parseInstant(s);
    }

    public TbDate(String s, String pattern) {
        this.instant = parseInstant(s, Locale.getDefault().toLanguageTag(), pattern);
    }

    public TbDate(String s, String pattern, String locale) {
        this.instant = parseInstant(s, locale, pattern);
    }

    public TbDate(String s, String pattern, String locale, String zoneId) {
        this.instant = parseInstant(s, pattern, locale, zoneId);
    }

    public TbDate(String s, String pattern, Locale locale, ZoneId zoneId) {
        instant =  parseInstant(s, pattern, locale, zoneId);
    }

    public TbDate(long dateMilliSecond) {
        instant = Instant.ofEpochMilli(dateMilliSecond);
    }

    public TbDate(int year, int month, int date) {
        this(year, month, date, 0, 0, 0, 0, null);
    }

    public TbDate(int year, int month, int date, String tz) {
        this(year, month, date, 0, 0, 0, 0, tz);
    }

    public TbDate(int year, int month, int date, int hrs, int min) {
        this(year, month, date, hrs, min, 0, 0, null);
    }

    public TbDate(int year, int month, int date, int hrs, int min, String tz) {
        this(year, month, date, hrs, min, 0, 0, tz);
    }

    public TbDate(int year, int month, int date, int hrs, int min, int second) {
        this(year, month, date, hrs, min, second, 0, null);
    }

    public TbDate(int year, int month, int date, int hrs, int min, int second, String tz) {
        this(year, month, date, hrs, min, second, 0, tz);
    }

    public TbDate(int year, int month, int date, int hrs, int min, int second, int milliSecond) {
        this(year, month, date, hrs, min, second, milliSecond, null);
    }

    public TbDate(int year, int month, int date, int hrs, int min, int second, int milliSecond, String tz) {
        ZoneId zoneId = tz != null && tz.length() > 0 ? ZoneId.of(tz) : ZoneId.systemDefault();
        instant = parseInstant(year, month, date, hrs, min, second,  milliSecond, zoneId);
    }

    public Instant getInstant() {
        return instant;
    }

    public ZonedDateTime getZonedDateTime() {
        return getZonedDateTime(zoneIdUTC);
    }
    public ZonedDateTime getZonedDateTime(ZoneId zoneId) {
        return instant.atZone(zoneId);
    }

    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.ofInstant(this.instant,  ZoneId.systemDefault());
    }

    public LocalDateTime getUTCDateTime() {
        return LocalDateTime.ofInstant(this.instant,  zoneIdUTC);
    }

    public String toDateString() {
        return toDateString(localeUTC.getLanguage());
    }
    public String toDateString(String locale) {
        return toDateString(locale, ZoneId.systemDefault().toString());
    }
    public String toDateString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
    }
    public String toTimeString() {
        return toTimeString(Locale.getDefault().getLanguage());
    }
    public String toTimeString(String locale) {
        return toTimeString(locale, ZoneId.systemDefault().toString());
    }
    public String toTimeString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL).withLocale(locale));
    }

    public String toISOString() {
        return instant.toString();
    }
    public String toJSON() {
        return toISOString();
    }
    public String toUTCString() {
        return toUTCString(localeUTC.getLanguage());
    }

    public String toUTCString(String localeStr) {
        return toLocaleString(localeStr, zoneIdUTC.getId(), (locale, options) -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM).withLocale(locale));
    }

    @JsonValue
    public String toString() {
        return toString(Locale.getDefault().getLanguage());
    }

    public String toString(String locale) {
        return toString(locale, ZoneId.systemDefault().toString());
    }

    public String toString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL).withLocale(locale));
    }

    public String toLocaleDateString() {
        return toLocaleDateString(localeUTC.getLanguage());
    }

    public String toLocaleDateString(String localeStr) {
        return toLocaleString(localeStr, ZoneId.systemDefault().toString(), (locale, options) -> DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
    }

    public String toLocaleDateString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> DateTimeFormatter.ofLocalizedDate(options.getDateStyle()).withLocale(locale));
    }

    public String toLocaleTimeString() {
        return toLocaleTimeString(Locale.getDefault().getLanguage());
    }

    public String toLocaleTimeString(String localeStr) {
        return toLocaleTimeString(localeStr, ZoneId.systemDefault().toString());
    }

    public String toLocaleTimeString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) -> DateTimeFormatter.ofLocalizedTime(options.getTimeStyle()).withLocale(locale));
    }

    public String toLocaleString() {
        return toLocaleString(localeUTC.getLanguage(), ZoneId.systemDefault().toString());
    }

    public String toLocaleString(String locale) {
        return toLocaleString(locale, ZoneId.systemDefault().toString());
    }

    public String toLocaleString(String localeStr, String optionsStr) {
        return toLocaleString(localeStr, optionsStr, (locale, options) ->
                DateTimeFormatter.ofLocalizedDateTime(options.getDateStyle(), options.getTimeStyle()).withLocale(locale));
    }

    public String toLocaleString(String localeStr, String optionsStr, BiFunction<Locale, DateTimeFormatOptions, DateTimeFormatter> formatterBuilder) {
        Locale locale = StringUtils.isNotEmpty(localeStr) ? Locale.forLanguageTag(localeStr) : Locale.getDefault();
        DateTimeFormatOptions options = getDateFormattingOptions(optionsStr);
        ZonedDateTime zdt = this.getInstant().atZone(options.getTimeZone().toZoneId());
        DateTimeFormatter formatter;
        if (StringUtils.isNotEmpty(options.getPattern())) {
            formatter = new DateTimeFormatterBuilder().appendPattern(options.getPattern()).toFormatter(locale);
        } else {
            formatter = formatterBuilder.apply(locale, options);
        }
        return formatter.format(zdt);
    }

    private DateTimeFormatOptions getDateFormattingOptions(String optionsStr) {
        DateTimeFormatOptions opt = null;
        if (StringUtils.isNotEmpty(optionsStr)) {
            try {
                opt = JacksonUtil.fromString(optionsStr, DateTimeFormatOptions.class);
            } catch (IllegalArgumentException iae) {
                opt = new DateTimeFormatOptions(optionsStr);
            }
        }
        if (opt == null) {
            opt = new DateTimeFormatOptions();
        }
        return opt;
    }

    public static long now() {
        return Instant.now().toEpochMilli();
    }
    public long parseSecond() {
        return instant.getEpochSecond();
    }

    public long parseSecondMilli() {
        return instant.toEpochMilli();
    }

    public static long UTC(int year) {
        return UTC(year, 0, 0, 0, 0, 0, 0);
    }
    public static long UTC(int year, int month) {
        return UTC(year, month, 0, 0, 0, 0, 0);
    }
    public static long UTC(int year, int month, int date) {
        return UTC(year, month, date, 0, 0, 0, 0);
    }
    public static long UTC(int year, int month, int date, int hrs) {
        return UTC(year, month, date, hrs, 0, 0, 0);
    }
    public static long UTC(int year, int month, int date, int hrs, int min) {
        return UTC(year, month, date, hrs, min, 0, 0);
    }
    public static long UTC(int year, int month, int date, int hrs, int min, int sec) {
        return UTC(year, month, date, hrs, min, sec, 0);
    }
    public static long UTC(int year, int month, int date, int hrs, int min, int sec, int ms) {
        year = year == 0 ? year = 1899 : year;
        month = month == 0 ? month = 12 : month;
        date = date == 0 ? date = 31 : date;
        return parseInstant(year, month, date, hrs, min, sec, ms, zoneIdUTC).toEpochMilli();
    }
    public int getUTCFullYear() {
        return getUTCDateTime().getYear();
    }

    public int getUTCMonth() {
        return getUTCDateTime().getMonthValue();
    }
    // day in month
    public int getUTCDate() {
        return getUTCDateTime().getDayOfMonth();
    }
    // day in week
    public int getUTCDay() {
        return getUTCDateTime().getDayOfWeek().getValue();
    }

    public int getUTCHours() {
        return getUTCDateTime().getHour();
    }

    public int getUTCMinutes() {
        return getZonedDateTime().getMinute();
    }

    public int getUTCSeconds() {
        return getUTCDateTime().getSecond();
    }
    public int getUTCMilliseconds() {
        return getUTCDateTime().getNano()/1000000;
    }

    public void setUTCFullYear(int year) {
        if (getUTCDate() > 28) {
            long time = getZonedDateTime().withYear(year).withDayOfMonth(1).toInstant().toEpochMilli() + (getUTCDate() - 1) * 24 * 60 * 60 * 1000L;
            this.instant = Instant.ofEpochMilli(time);
        } else {
            this.instant = getZonedDateTime().withYear(year).toInstant();
        }
    }
    public void setUTCFullYear(int year, int month) {
        if (getUTCDate() > 28) {
            long time = getZonedDateTime().withYear(year).withMonth(month).withDayOfMonth(1).toInstant().toEpochMilli() + (getUTCDate() - 1) * 24 * 60 * 60 * 1000L;
            this.instant = Instant.ofEpochMilli(time);
        } else {
            this.instant = getZonedDateTime().withYear(year).withMonth(month).toInstant();
        }
    }
    public void setUTCFullYear(int year, int month, int date) {
        this.instant = getZonedDateTime().withYear(year).withMonth(month).withDayOfMonth(date).toInstant();
    }
    public void setUTCMonth(int month) {
        if (getUTCDate() > 28) {
            long time = getZonedDateTime().withMonth(month).withDayOfMonth(1).toInstant().toEpochMilli() + (getUTCDate() - 1) * 24 * 60 * 60 * 1000L;
            this.instant = Instant.ofEpochMilli(time);
        } else {
            this.instant = getZonedDateTime().withMonth(month).toInstant();
        }
    }
    public void setUTCMonth(int month, int date) {
        this.instant = getZonedDateTime().withMonth(month).withDayOfMonth(date).toInstant();
    }
    public void setUTCDate(int date) {
        this.instant = getZonedDateTime().withDayOfMonth(date).toInstant();
    }
    public void setUTCHours(int hrs) {
        this.instant = getZonedDateTime().withHour(hrs).toInstant();
    }
    public void setUTCHours(int hrs, int minutes) {
        this.instant = getZonedDateTime().withHour(hrs).withMinute(minutes).toInstant();
    }
    public void setUTCHours(int hrs, int minutes, int seconds) {
        this.instant = getZonedDateTime().withHour(hrs).withMinute(minutes).withSecond(seconds).toInstant();
    }
    public void setUTCHours(int hrs, int minutes, int seconds, int ms) {
        this.instant = getZonedDateTime().withHour(hrs).withMinute(minutes).withSecond(seconds).withNano(ms*1000000).toInstant();
    }
    public void setUTCMinutes(int minutes) {
        this.instant = getZonedDateTime().withMinute(minutes).toInstant();
    }
    public void setUTCMinutes(int minutes, int seconds) {
        this.instant = getZonedDateTime().withMinute(minutes).withSecond(seconds).toInstant();    }
    public void setUTCMinutes(int minutes, int seconds, int ms) {
        this.instant = parseInstant(getUTCFullYear(), getUTCMonth(), getUTCDate(), getUTCHours(), minutes, seconds, ms, zoneIdUTC);
    }
    public void setUTCSeconds(int seconds) {
        this.instant = getZonedDateTime().withSecond(seconds).toInstant();
    }
    public void setUTCSeconds(int seconds, int ms) {
        this.instant = getZonedDateTime().withSecond(seconds).withNano(ms*1000000).toInstant();
    }
    public void setUTCMilliseconds(int ms) {
        this.instant = getZonedDateTime().withNano(ms*1000000).toInstant();
    }
    public int getFullYear() {
        return getLocalDateTime().getYear();
    }

    public int getMonth() {
        return getLocalDateTime().getMonthValue();
    }
    // day in month
    public int getDate() {
        return getLocalDateTime().getDayOfMonth();
    }
    // day in week
    public int getDay() {
        return getLocalDateTime().getDayOfWeek().getValue();
    }

    public int getHours() {
        return getLocalDateTime().getHour();
    }

    public int getMinutes() {
        return getLocalDateTime().getMinute();
    }

    public int getSeconds() {
        return getLocalDateTime().getSecond();
    }
    public int getMilliseconds() {
        return getLocalDateTime().getNano()/1000000;
    }
    // Milliseconds since Jan 1, 1970, 00:00:00.000 GMT
    public long getTime() {
        return instant.toEpochMilli();
    }
    public long valueOf(){
        return getTime() ;
    }
    public void setFullYear(int year) {
        Instant instantEpochWithYear = getZonedDateTime().withYear(year).toInstant();
        if (getDate() > 28) {
            long time = getLocalDateTime().withYear(year).withDayOfMonth(1).toInstant(getLocaleZoneOffset(instantEpochWithYear)).toEpochMilli() + (getDate() - 1) * 24 * 60 * 60 * 1000L;
            this.instant = Instant.ofEpochMilli(time);
        } else {
            this.instant = getLocalDateTime().withYear(year).toInstant(getLocaleZoneOffset(instantEpochWithYear));
        }
    }
    public void setFullYear(int year, int month) {
        Instant instantEpochWithYear = getZonedDateTime().withYear(year).withMonth(month).toInstant();
        if (getDate() > 28) {
            long time = getLocalDateTime().withYear(year).withMonth(month).withDayOfMonth(1).toInstant(getLocaleZoneOffset(instantEpochWithYear)).toEpochMilli() + (getDate() - 1) * 24 * 60 * 60 * 1000L;
            this.instant = Instant.ofEpochMilli(time);
        } else {
            this.instant = getLocalDateTime().withYear(year).withMonth(month).toInstant(getLocaleZoneOffset(instantEpochWithYear));
        }
    }
    public void setFullYear(int year, int month, int date) {
        Instant instantEpochWithYearMonthDate = getZonedDateTime().withYear(year).withMonth(month).withDayOfMonth(date).toInstant();
        this.instant = getLocalDateTime().withYear(year).withMonth(month).withDayOfMonth(date).toInstant(getLocaleZoneOffset(instantEpochWithYearMonthDate));
    }
    public void setMonth(int month) {
        Instant instantEpochWithYear = getZonedDateTime().withMonth(month).toInstant();
        if (getDate() > 28) {
            long time = getLocalDateTime().withMonth(month).withDayOfMonth(1).toInstant(getLocaleZoneOffset(instantEpochWithYear)).toEpochMilli() + (getDate() - 1) * 24 * 60 * 60 * 1000L;
            this.instant = Instant.ofEpochMilli(time);
        } else {
            this.instant = getLocalDateTime().withMonth(month).toInstant(getLocaleZoneOffset(instantEpochWithYear));
        }

    }
    public void setMonth(int month, int date) {
        Instant instantEpochWithMonthDate = getZonedDateTime().withMonth(month).withDayOfMonth(date).toInstant();
        this.instant = getLocalDateTime().withMonth(month).withDayOfMonth(date).toInstant(getLocaleZoneOffset(instantEpochWithMonthDate));
    }
    public void setDate(int date) {
        Instant instantEpochWithDate = getZonedDateTime().withDayOfMonth(date).toInstant();
        this.instant = getLocalDateTime().withDayOfMonth(date).toInstant(getLocaleZoneOffset(instantEpochWithDate));
    }
    public void setHours(int hrs) {
        this.instant = getLocalDateTime().withHour(hrs).toInstant(getLocaleZoneOffset(this.instant));
    }
    public void setHours(int hrs, int minutes) {
        this.instant = getLocalDateTime().withHour(hrs).withMinute(minutes).toInstant(getLocaleZoneOffset(this.instant));
    }
    public void setHours(int hrs, int minutes, int seconds) {
        this.instant = getLocalDateTime().withHour(hrs).withMinute(minutes).withSecond(seconds).toInstant(getLocaleZoneOffset(this.instant));
    }
    public void setHours(int hrs, int minutes, int seconds, int ms) {
        this.instant = getLocalDateTime().withHour(hrs).withMinute(minutes).withSecond(seconds).withNano(ms*1000000).toInstant(getLocaleZoneOffset(this.instant));
    }
    public void setMinutes(int minutes) {
        this.instant = getLocalDateTime().withMinute(minutes).toInstant(getLocaleZoneOffset(this.instant));
    }
    public void setMinutes(int minutes, int seconds) {
        this.instant = getLocalDateTime().withMinute(minutes).withSecond(seconds).toInstant(getLocaleZoneOffset(this.instant));

    }
    public void setMinutes(int minutes, int seconds, int ms) {
        this.instant = getLocalDateTime().withMinute(minutes).withSecond(seconds).withNano(ms*1000000).toInstant(getLocaleZoneOffset(this.instant));
    }
    public void setSeconds(int seconds) {
        this.instant = getLocalDateTime().withSecond(seconds).toInstant(getLocaleZoneOffset(this.instant));
    }
    public void setSeconds(int seconds, int ms) {
        this.instant = getLocalDateTime().withSecond(seconds).withNano(ms*1000000).toInstant(getLocaleZoneOffset(this.instant));
    }
    public void setMilliseconds(int ms) {
        this.instant = getLocalDateTime().withNano(ms*1000000).toInstant(getLocaleZoneOffset(this.instant));
    }

    // Milliseconds since Jan 1, 1970, 00:00:00.000 GMT
    public void setTime(long dateMilliSecond) {
        instant = Instant.ofEpochMilli(dateMilliSecond);
    }

    public void addDays(int days) {
        adjustTime(zonedDateTime -> zonedDateTime.plusDays(days));
    }

    public void addYears(int years) {
        adjustTime(zonedDateTime -> zonedDateTime.plusYears(years));
    }

    public void addMonths(int months) {
        adjustTime(zonedDateTime -> zonedDateTime.plusMonths(months));
    }

    public void addWeeks(int weeks) {
        adjustTime(zonedDateTime -> zonedDateTime.plusWeeks(weeks));
    }

    public void addHours(int hours) {
        adjustTime(zonedDateTime -> zonedDateTime.plusHours(hours));
    }

    public void addMinutes(int minutes) {
        adjustTime(zonedDateTime -> zonedDateTime.plusMinutes(minutes));
    }

    public void addSeconds(int seconds) {
        adjustTime(zonedDateTime -> zonedDateTime.plusSeconds(seconds));
    }

    public void addNanos(long nanos) {
        adjustTime(zonedDateTime -> zonedDateTime.plusNanos(nanos));
    }

    private void adjustTime(Function<ZonedDateTime, ZonedDateTime> adjuster) {
        ZonedDateTime zonedDateTime = adjuster.apply(getZonedDateTime());
        this.instant = zonedDateTime.toInstant();
    }

    public ZoneOffset getLocaleZoneOffset(Instant... instants){
        return ZoneId.systemDefault().getRules().getOffset(instants.length > 0 ? instants[0] : this.instant);
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
            return -1;
        }
    }

    private static Instant parseInstant(String s) {
        boolean isIsoFormat = s.length() > 0 && Character.isDigit(s.charAt(0));
        if (isIsoFormat) {
            return getInstant_ISO_OFFSET_DATE_TIME(s);
        } else {
            return getInstant_RFC_1123(s);
        }
    }

    private static Instant parseInstant(String s, String localeStr, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(localeStr));
            return Instant.from(formatter.parse(s));
        } catch (Exception ex) {
            try {
                return parseInstant(s, pattern, localeStr, ZoneId.systemDefault().getId());
            } catch (final DateTimeParseException e) {
                final ConversionException exception = new ConversionException("Cannot parse value [" + s + "] as instant", ex);
                throw exception;
            }
        }
    }

    private static Instant parseInstant(int year, int month, int date, int hrs, int min, int second, int secondMilli, ZoneId zoneId) {
        year = year < 70 ? 2000 + year : year <= 99 ? 1900 + year : year;
        ZonedDateTime zonedDateTime = ZonedDateTime.of(year, month, date, hrs, min, second, secondMilli*1000000, zoneId);
        return zonedDateTime.toInstant();
    }
    private static Instant parseInstant(String s, String pattern, String localeStr, String zoneIdStr) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(localeStr));
        LocalDateTime localDateTime = LocalDateTime.parse(s, dateTimeFormatter);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(zoneIdStr));
        return zonedDateTime.toInstant();
    }

    private static Instant getInstant_ISO_OFFSET_DATE_TIME(String s) {
        // assuming  "2007-12-03T10:15:30.00Z"  UTC instant
        // assuming  "2007-12-03T10:15:30.00"  ZoneId.systemDefault() instant
        // assuming  "2007-12-03T10:15:30.00-04:00"  TZ instant
        // assuming  "2007-12-03T10:15:30.00+04:00"  TZ instant
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        try {
            return Instant.from(formatter.parse(s));
        } catch (DateTimeParseException ex) {
            try {
                long timeMS = parse(s);
                if (timeMS != -1) {
                    return Instant.ofEpochMilli(timeMS);
                } else {
                    throw new ConversionException("Cannot parse value [" + s + "] as instant");
                }
            } catch (final DateTimeParseException e) {
                throw new ConversionException("Cannot parse value [" + s + "] as instant");
            }
        }
    }
    private static Instant getInstant_RFC_1123(String s) {
        // assuming RFC-1123 value "Tue, 3 Jun 2008 11:05:30 GMT"
        // assuming RFC-1123 value "Tue, 3 Jun 2008 11:05:30 GMT-02:00"
        // assuming RFC-1123 value "Tue, 3 Jun 2008 11:05:30 -0200"
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        try {
            return Instant.from(formatter.parse(s));
        } catch (DateTimeParseException ex) {
            try {
                return getInstantWithLocalZoneOffsetId_RFC_1123(s);
            } catch (final DateTimeParseException e) {
                throw new ConversionException("Cannot parse value [" + s + "] as instant");
            }
        }
    }
    private static Instant getInstantWithLocalZoneOffsetId_RFC_1123(String value) {
        String s = value.trim() + " GMT";
        Instant instant = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(s));
        ZoneId systemZone = ZoneId.systemDefault(); // my timezone
        String id =  systemZone.getRules().getOffset(instant).getId();
        value =  value.trim() + " " + id.replaceAll(":", "");
        return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(value));
    }

    private static Instant parseInstant(String s, String pattern, Locale locale, ZoneId zoneId) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern, locale);
        LocalDateTime localDateTime = LocalDateTime.parse(s, dateTimeFormatter);
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
        return zonedDateTime.toInstant();
    }
}
