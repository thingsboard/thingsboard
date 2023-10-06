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

import java.io.Serializable;
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
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.BiFunction;

public class TbDate implements Serializable, Cloneable {

    private Instant instant;
    private ZonedDateTime zonedDateTime;
    private LocalDateTime localDateTime;
    private final ZoneId zoneIdUTC = ZoneId.of("UTC");

    private static String patternDefault = "%s-%s-%sT%s:%s:%s.%s%s";

    public TbDate() {
        this.instant = Instant.now();
        initZonedLocalDateTime();
    }

    public TbDate(String s) {
        parseInstant(s);
    }

    /**
     * String s = "09:15:30 PM, Sun 10/09/2022";
     * String pattern = "hh:mm:ss a, EEE M/d/uuuu";
     * @param s
     * @param pattern
     */
    public TbDate(String s, String pattern, Locale locale) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern, locale);
        LocalDateTime localDateTime = LocalDateTime.parse(s, dateTimeFormatter);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        instant = zonedDateTime.toInstant();
        initZonedLocalDateTime();
    }

    public TbDate(long dateMilliSecond) {
        instant = Instant.ofEpochMilli(dateMilliSecond);
        initZonedLocalDateTime();
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
        this(createDateTimeFromPattern(year, month, date, hrs, min, second, secondMilli, tz));
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
       this.instant = instant;
    }
    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    private void initZonedLocalDateTime() {
        setZonedDateTime(zoneIdUTC);
        setLocalDateTime();
    }
    public void setZonedDateTime(ZoneId z) {
       this.zonedDateTime = instant.atZone(z);;
    }

    public void setLocalDateTime() {
        this.localDateTime = LocalDateTime.ofInstant(this.getInstant(),  ZoneId.systemDefault());
    }
    public LocalDateTime getLocalDateTime() {
        return this.localDateTime ;
    }

    public String toDateString() {
        return toLocaleDateString("UTC", JacksonUtil.newObjectNode().put("dateStyle", "full").toString());
    }
    public String toDateString(String locale) {
        return toLocaleDateString(locale, JacksonUtil.newObjectNode().put("dateStyle", "full").toString());
    }
    public String toTimeString() {
        return toLocaleTimeString("UTC", JacksonUtil.newObjectNode().put("timeStyle", "full").toString());
    }
    public String toTimeString(String locale) {
        return toLocaleTimeString(locale, JacksonUtil.newObjectNode().put("timeStyle", "full").toString());
    }
    /**
     * "2011-10-05T14:48:00.000Z"
     * @return
     */
    public String toISOString() {
        return instant.toString();
    }
    public String toJSON() {
        return toISOString();
    }
    public String toUTCString() {
        return toLocaleString("UTC", JacksonUtil.newObjectNode().put("dateStyle", "full").put("timeStyle", "medium").toString());
    }

    public String toUTCString(String locale) {
        return toLocaleString(locale, JacksonUtil.newObjectNode().put("dateStyle", "full").put("timeStyle", "medium").toString());
    }

    /**
     * "Tue Aug 19 1975 23:15:30 GMT+0300 (за східноєвропейським стандартним часом)"
     * @return
     */
    public String toString() {
        return toLocaleString(Locale.getDefault().toString(), JacksonUtil.newObjectNode().put("dateStyle", "full").put("timeStyle", "full").toString());
    }
    public String toString(String locale) {
        return toLocaleString(locale, JacksonUtil.newObjectNode().put("dateStyle", "full").put("timeStyle", "full").toString());
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
        ZonedDateTime zdt = this.getInstant().atZone(options.getTimeZone().toZoneId());
        DateTimeFormatter formatter;
        if (StringUtils.isNotEmpty(options.getPattern())) {
            formatter = new DateTimeFormatterBuilder().appendPattern(options.getPattern()).toFormatter(locale);
        } else {
            formatter = formatterBuilder.apply(locale, options);
        }
        return formatter.format(zdt);
    }

    private DateTimeFormatOptions getDateFormattingOptions(String options) {
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

    public long now() {
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
        return Instant.parse(createDateTimeFromPattern (year, month, date, hrs, min, sec, ms)).toEpochMilli();
    }
    public int getUTCFullYear() {
        return zonedDateTime.getYear();
    }

    public int getUTCMonth() {
        return zonedDateTime.getMonthValue();
    }
    // day in month
    public int getUTCDate() {
        return zonedDateTime.getDayOfMonth();
    }
    // day in week
    public int getUTCDay() {
       return zonedDateTime.getDayOfWeek().getValue();
    }

    public int getUTCHours() {
       return zonedDateTime.getHour();
    }

    public int getUTCMinutes() {
       return zonedDateTime.getMinute();
    }

    public int getUTCSeconds() {
       return zonedDateTime.getSecond();
    }
    public int getUTCMilliseconds() {
       return zonedDateTime.getNano()/1000000;
    }

    public void setUTCFullYear(int year) {
        parseInstant(createDateTimeFromPattern(year, getUTCMonth(), getUTCDate(), getUTCHours(), getUTCMinutes(), getUTCSeconds(), getUTCMilliseconds()));
    }
    public void setUTCFullYear(int year, int month) {
        parseInstant(createDateTimeFromPattern(year, month, getUTCDate(), getUTCHours(), getUTCMinutes(), getUTCSeconds(), getUTCMilliseconds()));
    }
    public void setUTCFullYear(int year, int month, int date) {
        parseInstant(createDateTimeFromPattern(year, month, date, getUTCHours(), getUTCMinutes(), getUTCSeconds(), getUTCMilliseconds()));
    }
    public void setUTCMonth(int month) {
        parseInstant(createDateTimeFromPattern(getUTCFullYear(), month, getUTCDate(), getUTCHours(), getUTCMinutes(), getUTCSeconds(), getUTCMilliseconds()));
    }
    public void setUTCMonth(int month, int date) {
        parseInstant(createDateTimeFromPattern(getUTCFullYear(), month, date, getUTCHours(), getUTCMinutes(), getUTCSeconds(), getUTCMilliseconds()));
    }
    public void setUTCDate(int date) {
        parseInstant(createDateTimeFromPattern(getUTCFullYear(), getUTCMonth(), date, getUTCHours(), getUTCMinutes(), getUTCSeconds(), getUTCMilliseconds()));
    }
    public void setUTCHours(int hrs) {
         parseInstant(createDateTimeFromPattern(getUTCFullYear(), getUTCMonth(), getUTCDate(), hrs, getUTCMinutes(), getUTCSeconds(), getUTCMilliseconds()));
    }
    public void setUTCHours(int hrs, int minutes) {
         parseInstant(createDateTimeFromPattern(getUTCFullYear(), getUTCMonth(), getUTCDate(), hrs, minutes, getUTCSeconds(), getUTCMilliseconds()));
    }
    public void setUTCHours(int hrs, int minutes, int seconds) {
         parseInstant(createDateTimeFromPattern(getUTCFullYear(), getUTCMonth(), getUTCDate(), hrs, minutes, seconds, getUTCMilliseconds()));
    }
    public void setUTCHours(int hrs, int minutes, int seconds, int ms) {
         parseInstant(createDateTimeFromPattern(getUTCFullYear(), getUTCMonth(), getUTCDate(), hrs, minutes, seconds, ms));
    }
    public void setUTCMinutes(int minutes) {
       parseInstant(createDateTimeFromPattern(getUTCFullYear(), getUTCMonth(), getUTCDate(), getUTCHours(), minutes, getUTCSeconds(), getUTCMilliseconds()));
    }
    public void setUTCMinutes(int minutes, int seconds) {
       parseInstant(createDateTimeFromPattern(getUTCFullYear(), getUTCMonth(), getUTCDate(), getUTCHours(), minutes, seconds, getUTCMilliseconds()));
    }
    public void setUTCMinutes(int minutes, int seconds, int ms) {
       parseInstant(createDateTimeFromPattern(getUTCFullYear(), getUTCMonth(), getUTCDate(), getUTCHours(), minutes, seconds, ms));
    }
    public void setUTCSeconds(int seconds) {
        parseInstant(createDateTimeFromPattern(getUTCFullYear(), getUTCMonth(), getUTCDate(), getUTCHours(), getUTCMinutes(), seconds, getUTCMilliseconds()));
    }
    public void setUTCSeconds(int seconds, int ms) {
        parseInstant(createDateTimeFromPattern(getUTCFullYear(), getUTCMonth(), getUTCDate(), getUTCHours(), getUTCMinutes(), seconds, ms));
    }
    public void setUTCMilliseconds(int ms) {
        parseInstant(createDateTimeFromPattern(getUTCFullYear(), getUTCMonth(), getUTCDate(), getUTCHours(), getUTCMinutes(), getUTCSeconds(), ms));
    }
    public int getFullYear() {
        return localDateTime.getYear();
    }

    public int getMonth() {
        return localDateTime.getMonthValue();
    }
    // day in month
    public int getDate() {
        return localDateTime.getDayOfMonth();
    }
    // day in week
    public int getDay() {
       return localDateTime.getDayOfWeek().getValue();
    }

    public int getHours() {
       return localDateTime.getHour();
    }

    public int getMinutes() {
       return localDateTime.getMinute();
    }

    public int getSeconds() {
       return localDateTime.getSecond();
    }
    public int getMilliseconds() {
        return localDateTime.getNano()/1000000;
    }
    // Milliseconds since Jan 1, 1970, 00:00:00.000 GMT
     public long getTime() {
        return instant.toEpochMilli();
    }
    public long valueOf(){
        return getTime() ;
    }
    public void setFullYear(int year) {
        setUTCFullYear(year);
    }
    public void setFullYear(int year, int month) {
        setUTCFullYear(year, month);
    }    public void setFullYear(int year, int month, int date) {
        setUTCFullYear(year, month, date);
    }
    public void setMonth(int month) {
        setUTCMonth(month);
    }
    public void setMonth(int month, int date) {
        setUTCMonth(month, date) ;
    }
    public void setDate(int date) {
        setUTCDate(date);
    }
    public void setHours(int hrs) {
        setUTCHours(hrs);
    }
    public void setHours(int hrs, int minutes) {
        setUTCHours(hrs, minutes);
    }
    public void setHours(int hrs, int minutes, int seconds) {
        setUTCHours(hrs, minutes, seconds);
    }
    public void setHours(int hrs, int minutes, int seconds, int ms) {
        setUTCHours(hrs, minutes, seconds, ms);
    }
    public void setMinutes(int minutes) {
        setUTCMinutes(minutes);
    }
    public void setMinutes(int minutes, int seconds) {
        setUTCMinutes(minutes, seconds);
    }
    public void setMinutes(int minutes, int seconds, int ms) {
        setUTCMinutes(minutes, seconds, ms);
    }
    public void setSeconds(int seconds) {
        setUTCSeconds(seconds);
    }
    public void setSeconds(int seconds, int ms) {
        setUTCSeconds(seconds, ms);
    }
    public void setMilliseconds(int ms) {
        setUTCMilliseconds(ms);
    }

    // Milliseconds since Jan 1, 1970, 00:00:00.000 GMT
     public void setTime(long dateMilliSecond) {
         instant = Instant.ofEpochMilli(dateMilliSecond);
         initZonedLocalDateTime();
    }
    public int getTimezoneOffset() {
        int seconds = ZoneId.systemDefault().getRules().getOffset(instant).getTotalSeconds();
        return -seconds/60;
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
        DateTimeFormatter isoDateFormatter = DateTimeFormatter.ofPattern(
                "yyyy-MM-dd[[ ]['T']HH:mm[:ss[.SSS]][ ][XXX][Z][z][VV][O]]").withZone(ZoneId.systemDefault());
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
    private static String createDateTimeFromPattern(int year, int month, int date, int hrs, int min, int second, int secondMilli, String... tz) {
        String yearStr = String.format("%04d", year);
        yearStr = yearStr.substring(0, 2).equals("00") ? year < 70 ? "20" + yearStr.substring(2,4) : "19" + yearStr.substring(2,4) : yearStr;
        String monthStr = String.format("%02d", month);
        String dateStr = String.format("%02d", date);
        String hrsStr = String.format("%02d", hrs);
        String minStr = String.format("%02d", min);
        String secondStr = String.format("%02d", second);
        String secondMilliStr = String.format("%03d", secondMilli);
        String tzStr = tz.length > 0 ? Arrays.stream(tz).findFirst().get() : "Z";
        return String.format(patternDefault, yearStr, monthStr, dateStr, hrsStr, minStr, secondStr, secondMilliStr, tzStr);
    }

    private void parseInstant(String s) {
        try{
            if (s.length() > 0 && Character.isDigit(s.charAt(0))) {
                // assuming UTC instant  "2007-12-03T10:15:30.00Z"
                instant = Instant.parse(s);
            }
            else {
                // assuming RFC-1123 value "Tue, 3 Jun 2008 11:05:30 GMT-02:00"
                instant = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(s));
            }
            initZonedLocalDateTime();
        } catch (final DateTimeParseException ex) {
            final ConversionException exception = new ConversionException("Cannot parse value [" + s + "] as instant", ex);
            throw exception;
        }
    }
}
