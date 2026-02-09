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

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.StringUtils;

import java.time.format.FormatStyle;
import java.util.TimeZone;

@NoArgsConstructor
@Data
class DateTimeFormatOptions {
    private static final TimeZone DEFAULT_TZ = TimeZone.getDefault();

    private String timeZone;
    private String dateStyle;
    private String timeStyle;
    @Getter
    private String pattern;

    public DateTimeFormatOptions(String timeZone) {
        this.timeZone = timeZone;
    }

    TimeZone getTimeZone() {
        return StringUtils.isNotEmpty(timeZone) ? TimeZone.getTimeZone(timeZone) : TimeZone.getDefault();
    }

    FormatStyle getDateStyle() {
        return getFormatStyle(dateStyle, FormatStyle.SHORT);
    }

    FormatStyle getTimeStyle() {
        return getFormatStyle(timeStyle, FormatStyle.MEDIUM);
    }

    private static FormatStyle getFormatStyle(String style, FormatStyle defaultStyle) {
        if (StringUtils.isNotEmpty(style)) {
            try {
                return FormatStyle.valueOf(style.toUpperCase());
            } catch (IllegalArgumentException e) {
                return defaultStyle;
            }
        } else {
            return defaultStyle;
        }
    }

}
