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

import lombok.Data;

@Data
public class TbDateTimeFormatOptions {
    String localeMatcher = "undefined"; // ?: "best fit" | "lookup" | undefined;
    String weekday = "undefined"; //?: "long" | "short" | "narrow" | undefined;
    String era = "undefined"; //?: "long" | "short" | "narrow" | undefined;
    String year = "undefined"; //?: "numeric" | "2-digit" | undefined;
    String month = "undefined"; //?: "numeric" | "2-digit" | "long" | "short" | "narrow" | undefined;
    String day = "undefined"; //?: "numeric" | "2-digit" | undefined;
    String hour = "undefined"; //?: "numeric" | "2-digit" | undefined;
    String minute = "undefined"; //?: "numeric" | "2-digit" | undefined;
    String second = "undefined"; //?: "numeric" | "2-digit" | undefined;
    String timeZoneName = "undefined"; //?: "full" |  "long" | "short" | undefined;
    String timeZoneNameNumeric= "undefined"; //?:  "numeric" | "long" | "middle" | "short" | undefined;
    String formatMatcher = "undefined"; //?: "best fit" | "basic" | undefined;
    boolean hour12 = false; //?: boolean | undefined;
    String timeZone = "undefined"; //?: string | undefined;
    char delimiter = '/'; // delimiter can be -, /, or .

    public String getPatternJava () {
        String pattern = "";
        String patternDate = "";
        String patternTime = "";
        delimiterValidation();

        if ("short".equals(weekday)) {
            patternDate = "E, ";
        } else if ("long".equals(weekday)) {
            patternDate = "EEEE, ";
        }
        if ("numeric".equals(day)) {
            patternDate += "d";
        } else if ("2-digit".equals(day)) {
            patternDate += "dd";
        }
        if ("numeric".equals(month)) {
            patternDate = patternDate.isEmpty() ? "M" :  patternDate + delimiter + "M";
        } else if ("2-digit".equals(month)) {
            patternDate = patternDate.isEmpty() ? "MM" :  patternDate+ delimiter + "MM";
        } else if ("short".equals(month)) {
            patternDate = patternDate.isEmpty() ? "MMM" :  patternDate+ delimiter + "MMM";
        } else if ("long".equals(month)) {
            patternDate = patternDate.isEmpty() ? "MMMM" :  patternDate+ delimiter + "MMMM";
        }

        if ("numeric".equals(year)) {
            patternDate = patternDate.isEmpty() ? "yyyy" :  patternDate+ delimiter + "yyyy";
        } else if ("2-digit".equals(year)) {
            patternDate = patternDate.isEmpty() ? "yy" :  patternDate + delimiter + "yy";
        }

        // hour12: false - 0 - 24h; hour12: true -0-12 AM/PM
        if ("numeric".equals(hour)) {
            patternTime = hour12 ? "h" : "H";
        } else if ("2-digit".equals(hour)) {
            patternTime = hour12 ? "hh" : "HH";
        }
        if ("numeric".equals(minute)) {
            patternTime = patternTime.isEmpty() ? "m" :  patternTime + ":m";
        } else if ("2-digit".equals(minute)) {
            patternTime = patternTime.isEmpty() ? "mm" :  patternTime + ":mm";
        }
        if ("numeric".equals(second)) {
            patternTime = patternTime.isEmpty() ? "s" :  patternTime + ":s";
        } else if ("2-digit".equals(second)) {
            patternTime = patternTime.isEmpty() ? "ss" :  patternTime + ":ss";
        }

        if (hour12 && !hour.equals("undefined")) {
            patternTime = patternTime + " a";
        }

        if ("long".equals(era) || "short".equals(era) || "narrow".equals(era)) {
            pattern =  (patternDate + " G " +  patternTime).trim();
        } else {
            pattern = (patternDate + " " + patternTime).trim();
        }

        /**
         * z/zzzz 	Time zone 	General time zone 	Pacific Standard Time; PST;
         * Z 	Time zone 	RFC 822 time zone 	-0800
         * X/XX/XXX 	Time zone 	ISO 8601 time zone 	-08; -0800; -08:00
         */
        if ("full".equals(timeZoneName)) {
            pattern += " zXXX (zzzz)";
        } else if ("short".equals(timeZoneName)) {
            pattern += " z";
        } else if ("long".equals(timeZoneName)){
            pattern += " zzzz";
        } else if ("short".equals(timeZoneNameNumeric)) {
            pattern += " X";
        } else if ("middle".equals(timeZoneNameNumeric)) {
            pattern += " XX";
        } else if ("long".equals(timeZoneNameNumeric)) {
            pattern += " XXX";
        }else if ("numeric".equals(timeZoneNameNumeric)) {
            pattern += " Z";
        }

        return pattern.isEmpty() ? null : pattern;
    }

    private void delimiterValidation() {
        if ("long".equals(month) || "short".equals(month)) {
            delimiter = ' ';
        } else if (!(delimiter == '/' ||  delimiter == '-' || delimiter == '.')) {
            delimiter = '/';
        }
    }
}
