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

public class ApiKeyExpiryWarningNotification implements Notification {

    private enum Urgency {EXPIRED, WITHIN_A_DAY, DAYS_LEFT}

    private final String apiKeyDescription;
    private final long daysLeft;
    private final Urgency urgency;

    private ApiKeyExpiryWarningNotification(String apiKeyDescription, long daysLeft, Urgency urgency) {
        this.apiKeyDescription = apiKeyDescription;
        this.daysLeft = daysLeft;
        this.urgency = urgency;
    }

    public static ApiKeyExpiryWarningNotification expired(String apiKeyDescription) {
        return new ApiKeyExpiryWarningNotification(apiKeyDescription, 0, Urgency.EXPIRED);
    }

    public static ApiKeyExpiryWarningNotification expiringWithinADay(String apiKeyDescription) {
        return new ApiKeyExpiryWarningNotification(apiKeyDescription, 0, Urgency.WITHIN_A_DAY);
    }

    public static ApiKeyExpiryWarningNotification expiringIn(String apiKeyDescription, long daysLeft) {
        return new ApiKeyExpiryWarningNotification(apiKeyDescription, daysLeft, Urgency.DAYS_LEFT);
    }

    @Override
    public String getText() {
        return switch (urgency) {
            case EXPIRED -> String.format(":rotating_light: API key '%s' is no longer valid (EXPIRED or revoked) - rotate it now to restore monitoring", apiKeyDescription);
            case WITHIN_A_DAY -> String.format(":rotating_light: API key '%s' expires within a day - rotate it now to avoid a monitoring outage", apiKeyDescription);
            case DAYS_LEFT -> String.format(":warning: API key '%s' expires in %d %s - rotate it soon to avoid a monitoring outage",
                    apiKeyDescription, daysLeft, daysLeft == 1 ? "day" : "days");
        };
    }

    @Override
    public boolean isIncident() {
        return false;
    }

}
