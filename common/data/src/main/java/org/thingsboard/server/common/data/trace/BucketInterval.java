/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.common.data.trace;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BucketInterval {
    AUTO("auto"),
    ONE_MINUTE("1m"),
    TWO_MINUTES("2m"),
    FIVE_MINUTES("5m"),
    TEN_MINUTES("10m"),
    FIFTEEN_MINUTES("15m"),
    THIRTY_MINUTES("30m"),
    ONE_HOUR("1h"),
    TWO_HOURS("2h"),
    FIVE_HOURS("5h"),
    ONE_DAY("day");

    private final String value;

    BucketInterval(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static BucketInterval fromString(String value) {
        if (value == null) {
            return AUTO;
        }

        for (BucketInterval interval : BucketInterval.values()) {
            if (interval.value.equalsIgnoreCase(value)) {
                return interval;
            }
        }

        throw new IllegalArgumentException("Invalid interval. Supported values: 1m, 2m, 5m, 10m, 15m, 30m, 1h, 2h, 5h, day");
    }

    public static BucketInterval determineAutoInterval(long durationMs) {
        long durationHours = durationMs / (60 * 60 * 1000);

        if (durationHours < 1) {
            return FIFTEEN_MINUTES;
        } else if (durationHours <= 24) {
            return ONE_HOUR;
        } else {
            return ONE_DAY;
        }
    }

    public long getDurationMs() {
        switch (this) {
            case ONE_MINUTE:     return 60_000L;
            case TWO_MINUTES:    return 2 * 60_000L;
            case FIVE_MINUTES:   return 5 * 60_000L;
            case TEN_MINUTES:    return 10 * 60_000L;
            case FIFTEEN_MINUTES:return 15 * 60_000L;
            case THIRTY_MINUTES: return 30 * 60_000L;
            case ONE_HOUR:       return 3_600_000L;
            case TWO_HOURS:      return 2 * 3_600_000L;
            case FIVE_HOURS:     return 5 * 3_600_000L;
            case ONE_DAY:        return 86_400_000L;
            default:             return -1;
        }
    }
}
