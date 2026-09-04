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
package org.thingsboard.monitoring.data.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyExpiryWarningNotificationTest {

    @Test
    void expiredFalse_generatesWarningMessageWithDaysCount() {
        ApiKeyExpiryWarningNotification notification = ApiKeyExpiryWarningNotification.expiringIn("my-api-key", 5);

        String text = notification.getText();

        assertThat(text)
                .contains("expires in 5 days")
                .contains("my-api-key")
                .contains(":warning:")
                .doesNotContain("EXPIRED");
    }

    @Test
    void expiredFalse_oneDayLeft_usesSingularDay() {
        ApiKeyExpiryWarningNotification notification = ApiKeyExpiryWarningNotification.expiringIn("my-api-key", 1);

        assertThat(notification.getText())
                .contains("expires in 1 day")
                .doesNotContain("1 days");
    }

    @Test
    void expiringWithinADay_generatesUrgentMessageWithoutMisleadingDayCount() {
        ApiKeyExpiryWarningNotification notification = ApiKeyExpiryWarningNotification.expiringWithinADay("my-api-key");

        String text = notification.getText();

        assertThat(text)
                .contains("expires within a day")
                .contains("my-api-key")
                .contains(":rotating_light:")
                .doesNotContain("expires in");
    }

    @Test
    void expiredTrue_generatesExpiredMessageWithoutDayCount() {
        ApiKeyExpiryWarningNotification notification = ApiKeyExpiryWarningNotification.expired("my-api-key");

        String text = notification.getText();

        assertThat(text)
                .contains("EXPIRED")
                .contains("my-api-key")
                .contains(":rotating_light:")
                .doesNotContain("expires in");
    }

}
