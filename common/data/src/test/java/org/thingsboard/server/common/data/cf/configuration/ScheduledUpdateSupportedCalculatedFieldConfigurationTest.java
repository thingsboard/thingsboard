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
