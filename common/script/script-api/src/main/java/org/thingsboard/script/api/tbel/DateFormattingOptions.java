package org.thingsboard.script.api.tbel;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.StringUtils;

import java.time.format.FormatStyle;
import java.util.TimeZone;

@NoArgsConstructor
@Data
class DateFormattingOptions {
    private static final TimeZone DEFAULT_TZ = TimeZone.getDefault();

    private String timeZone;
    private String dateStyle;
    private String timeStyle;
    @Getter
    private String pattern;

    public DateFormattingOptions(String timeZone) {
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
