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

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApiKeyExpiryWarningNotification implements Notification {

    private final String apiKeyDescription;
    private final long daysLeft;
    private final boolean expired;

    @Override
    public String getText() {
        if (expired) {
            return String.format(":rotating_light: API key '%s' has EXPIRED - rotate it now to restore monitoring", apiKeyDescription);
        }
        return String.format(":warning: API key '%s' expires in %d day(s) - rotate it soon to avoid a monitoring outage", apiKeyDescription, daysLeft);
    }

    @Override
    public boolean isIncident() {
        return false;
    }

}
