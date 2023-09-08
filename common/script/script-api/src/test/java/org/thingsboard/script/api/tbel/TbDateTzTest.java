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

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mvel2.ExecutionContext;
import org.mvel2.ParserContext;
import org.mvel2.SandboxedParserConfiguration;
import org.mvel2.execution.ExecutionHashMap;
import java.text.ParseException;
import java.util.Date;

@Slf4j

public class TbDateTzTest {

    private ExecutionContext ctx;

    @Before
    public void before() {
        SandboxedParserConfiguration parserConfig = ParserContext.enableSandboxedMode();
        ctx = new ExecutionContext(parserConfig);
        Assert.assertNotNull(ctx);
    }

    @After
    public void after() {
        ctx.stop();
    }

    @Test
    public void testToLocaleStringTz() throws ParseException {
//        TimeZone tz = TimeZone.getDefault();
//        TimeZone.setDefault(tz);
//        Calendar cal = Calendar.getInstance(tz, Locale.getDefault(Locale.Category.FORMAT));
//        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault(Locale.Category.FORMAT));
//        Date dateBeforeDST = df.parse("2018-03-25 01:55");
//        cal.setTime(dateBeforeDST);
//        assertThat(cal.get(Calendar.ZONE_OFFSET)).isEqualTo(3600000);
//        assertThat(cal.get(Calendar.DST_OFFSET)).isEqualTo(0);
        TbDate d = new TbDate(2023, 8, 6, 4, 4, 5); //  1693962245000L
        String expected = "6 вер. 2023 р.";
        String expectedUTC = "06/09/2023 01:04:05";
        String expectedSE = "06/09/2023 08:04:05";
        String expectedUS= "05/09/2023 21:04:05";
        String expectedUA= "06/09/2023 04:04:05";
        String actual = d.toDateString();
        String actualUTC = d.toLocaleString("en-US");
        String actualSE = d.toLocaleString("sv-SE",  "Asia/Jakarta");       // UTC + 07:00
        String actualUS = d.toLocaleString("en-US", "America/New_York");    // UTC - 04:00
        String actualUA = d.toLocaleString("uk-UA", "Europe/Kiev");         // UTC+02:00 and in summer as UTC+03:00
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(expectedUTC, actualUTC);
        Assert.assertEquals(expectedUS, actualUS);
        Assert.assertEquals(expectedSE, actualSE);
        Assert.assertEquals(expectedUA, actualUA);
    }

    @Test
    public void testToLocaleStringTzPattern() throws ParseException {
        TbDate d = new TbDate(2023, 8, 6, 4, 4, 5); //  1693962245000L
        String localString = "uk-UA";
        ExecutionHashMap<String, Object> pattern = getPattern();
        String expectedUA = "ср, 6 вересня 2023 н. е. 4:4:5 дп EEST+03:00 (за східноєвропейським літнім часом)";
        String actualUA = d.toLocaleString(localString, pattern);
        Assert.assertEquals(expectedUA, actualUA);

        String expectedUTC_AM = "6 вер. 2023 1:4:5 дп UTC";
        pattern.put("timeZone", "UTC");
        pattern.put("timeZoneName", "short");
        pattern.put("weekday", "undefined");
        pattern.put("month", "short");
        pattern.put("era", "undefined");
        String actualUTC_AM = d.toLocaleString(localString, pattern);
        Assert.assertEquals(expectedUTC_AM, actualUTC_AM);

        String expectedUTC = "06/09/2023 01:04:05 +0000";
        pattern.remove("timeZoneName", "short");
        pattern.put("timeZoneNameNumeric", "numeric");
        pattern.remove("delimiter", ".");
        pattern.put("month", "2-digit");
        pattern.put("day", "2-digit");
        pattern.put("hour", "2-digit");
        pattern.put("minute", "2-digit");
        pattern.put("second", "2-digit");
        pattern.put("hour12", false);
        String actualUTC = d.toLocaleString(pattern);
        Assert.assertEquals(expectedUTC, actualUTC);

        String expectedUS_AM = "Tuesday, 05/9/23 09:04:05 PM -0400";
        pattern.put("timeZone", "America/New_York");
        pattern.put("weekday", "long");
        pattern.put("timeZoneNameNumeric", "middle");
        pattern.remove("delimiter", ".");
        pattern.put("year", "2-digit");
        pattern.put("month", "numeric");
        pattern.put("hour12", true);
        localString = "en-US";
        String actualUS_AM = d.toLocaleString(localString, pattern);
        Assert.assertEquals(expectedUS_AM, actualUS_AM);

        String expectedUS = "05 Sep 2023 21:04:05 -04:00";
        pattern.remove("weekday", "long");
        pattern.put("timeZoneNameNumeric", "long");
        pattern.remove("delimiter", ".");
        pattern.put("year", "numeric");
        pattern.put("month", "short");
        pattern.put("day", "2-digit");
        pattern.put("hour12", false);
        String actualUS = d.toLocaleString(localString, pattern);
        Assert.assertEquals(expectedUS, actualUS);
    }

    @Test
    public void testToLocaleStringTzPattern_de_DE_ar_EG() throws ParseException {
//        Date dUTC = new Date(Date.UTC(2012, 11, 20, 3, 0, 0)); // 1693962245000L
//        TbDate d = new TbDate(dUTC.getTime()); //  1693962245000L
        TbDate d = new TbDate(2012, 11, 20, 3, 0, 0);   // 1355965200000
        String localString = "de-DE";
        ExecutionHashMap<String, Object> pattern = getPattern();
        pattern.put("weekday", "long");
        pattern.put("hour12", false);
        pattern.put("timeZone", "UTC");
        pattern.remove("hour", "numeric");
        pattern.remove("minute", "numeric");
        pattern.remove("second", "numeric");
        pattern.remove("timeZoneName", "full");
        pattern.remove("delimiter", ".");
        pattern.remove("era", "long");
        String expected = "Donnerstag, 20 Dezember 2012";
        String actual = d.toLocaleString(localString, pattern);
        Assert.assertEquals(expected, actual);

        localString = "ar-EG";
        expected = "الخميس, ٢٠ ديسمبر ٢٠١٢";
        actual = d.toLocaleString(localString, pattern);
        Assert.assertEquals(expected, actual);
    }

    private ExecutionHashMap<String, Object> getPattern() {
        ExecutionHashMap<String, Object> pattern = new ExecutionHashMap<>(1, ctx);
        pattern.put("timeZone", "Europe/Kiev");
        pattern.put("timeZoneName", "full");
        pattern.put("delimiter", ".");
        pattern.put("era", "long");
        pattern.put("weekday", "short");
        pattern.put("year", "numeric");
        pattern.put("month", "long");
        pattern.put("day", "numeric");
        pattern.put("hour", "numeric");
        pattern.put("minute", "numeric");
        pattern.put("second", "numeric");
        pattern.put("hour12", true);
        return pattern;
    }
}

