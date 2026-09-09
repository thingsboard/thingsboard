// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class ScheduledUpdateSupportedCalculatedFieldConfigurationTest {

    @Test
    void validateDoesNotThrowAnyExceptionWhenScheduledUpdateIntervalIsGreaterThanMinAllowedIntervalInTenantProfile() {
        int scheduledUpdateInterval = 60;
        int minAllowedInterval = scheduledUpdateInterval - 1;

        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setScheduledUpdateInterval(scheduledUpdateInterval);
        assertThatCode(() -> cfg.validate(minAllowedInterval)).doesNotThrowAnyException();
    }

    @Test
    void validateShouldThrowWhenScheduledUpdateIntervalIsLessThanMinAllowedIntervalInTenantProfile() {
        int minAllowedInterval = (int) TimeUnit.HOURS.toSeconds(2);

        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setScheduledUpdateInterval(1);

        assertThatThrownBy(() -> cfg.validate(minAllowedInterval))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheduled update interval (1 seconds) is less than " +
                            "minimum allowed interval in tenant profile: " + minAllowedInterval + " seconds");
    }

}
