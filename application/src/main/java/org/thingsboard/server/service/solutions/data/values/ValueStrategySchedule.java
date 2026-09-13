// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;

import lombok.Data;

@Data
public class ValueStrategySchedule {

    private int startHour;
    private int startMinute;

    private int endHour;
    private int endMinute;

    private ValueStrategyDefinition definition;

}
