// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import jakarta.validation.constraints.PositiveOrZero;

public interface ScheduledUpdateSupportedCalculatedFieldConfiguration extends CalculatedFieldConfiguration {

    boolean isScheduledUpdateEnabled();

    @PositiveOrZero
    Integer getScheduledUpdateInterval();

    void setScheduledUpdateInterval(Integer interval);

    default void validate(long minAllowedScheduledUpdateInterval) {
        if (getScheduledUpdateInterval() < minAllowedScheduledUpdateInterval) {
            throw new IllegalArgumentException("Scheduled update interval (" + getScheduledUpdateInterval() +
                    " seconds) is less than minimum allowed interval in tenant profile: " + minAllowedScheduledUpdateInterval + " seconds");
        }
    }
}
