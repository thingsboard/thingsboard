/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.cf.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.thingsboard.server.common.data.cf.configuration.ScheduledUpdateSupportedCalculatedFieldConfiguration.SUPPORTED_TIME_UNITS;

@ExtendWith(MockitoExtension.class)
class ScheduledUpdateSupportedCalculatedFieldConfigurationTest {

    @ParameterizedTest
    @EnumSource(TimeUnit.class)
    void validateShouldThrowWhenScheduledUpdateIntervalIsSetButTimeUnitIsNotSupported(TimeUnit timeUnit) {
        int scheduledUpdateInterval = 60;
        int minAllowedInterval = (int) timeUnit.toSeconds(scheduledUpdateInterval - 1);

        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setScheduledUpdateInterval(scheduledUpdateInterval);
        cfg.setTimeUnit(timeUnit);

        if (SUPPORTED_TIME_UNITS.contains(timeUnit)) {
            assertThatCode(() -> cfg.validate(minAllowedInterval)).doesNotThrowAnyException();
            return;
        }
        assertThatThrownBy(() -> cfg.validate(minAllowedInterval))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported scheduled update time unit: " + timeUnit + ". Allowed: " + SUPPORTED_TIME_UNITS);
    }

    @Test
    void validateShouldThrowWhenScheduledUpdateIntervalIsSetButTimeUnitIsNotSpecified() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setScheduledUpdateInterval(60);
        cfg.setTimeUnit(null);

        assertThatThrownBy(() -> cfg.validate(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheduled update time unit should be specified!");
    }

    @Test
    void validateShouldThrowWhenScheduledUpdateIntervalIsLessThanMinAllowedIntervalInTenantProfile() {
        int minAllowedInterval = (int) TimeUnit.HOURS.toSeconds(2);

        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setScheduledUpdateInterval(1);
        cfg.setTimeUnit(TimeUnit.HOURS);

        assertThatThrownBy(() -> cfg.validate(minAllowedInterval))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheduled update interval is less than configured " +
                            "minimum allowed interval in tenant profile: " + minAllowedInterval);
    }

}
