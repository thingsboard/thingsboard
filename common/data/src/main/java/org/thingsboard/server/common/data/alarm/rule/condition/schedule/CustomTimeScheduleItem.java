// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.schedule;

import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema
@Data
public class CustomTimeScheduleItem implements Serializable {

    private boolean enabled;
    private int dayOfWeek;
    private long startsOn;
    private long endsOn;

}
