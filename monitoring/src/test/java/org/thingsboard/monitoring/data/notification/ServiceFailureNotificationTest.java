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

class ServiceFailureNotificationTest {

    @Test
    void stripResponseBodyRemovesNginxErrorHtml() {
        String msg = "503 Service Temporarily Unavailable on POST request for \"https://domain/api/auth/login\": \""
                + "<html><head><title>503 Service Temporarily Unavailable</title></head>"
                + "<body><center><h1>503 Service Temporarily Unavailable</h1></center><hr><center>nginx</center></body></html>\"";

        String sanitized = ServiceFailureNotification.stripResponseBody(msg);

        assertThat(sanitized)
                .isEqualTo("503 Service Temporarily Unavailable on POST request for \"https://domain/api/auth/login\"");
    }

    @Test
    void stripResponseBodyRemovesDoctypeHtml() {
        String msg = "500 Internal Server Error: \"<!DOCTYPE html><html>...</html>\"";

        String sanitized = ServiceFailureNotification.stripResponseBody(msg);

        assertThat(sanitized).isEqualTo("500 Internal Server Error");
    }

    @Test
    void stripResponseBodyLeavesPlainMessagesUntouched() {
        String msg = "Connection refused";
        assertThat(ServiceFailureNotification.stripResponseBody(msg)).isEqualTo(msg);
    }

    @Test
    void stripResponseBodyHandlesNull() {
        assertThat(ServiceFailureNotification.stripResponseBody(null)).isNull();
    }

    @Test
    void linkifyReplacesRequestForUrlWithSlackMrkdwnLink() {
        String msg = "503 Service Temporarily Unavailable on POST request for \"https://example.com/api/auth/login\"";

        assertThat(ServiceFailureNotification.linkifyRequestUrl(msg))
                .isEqualTo("503 Service Temporarily Unavailable on POST <https://example.com/api/auth/login|request>");
    }

    @Test
    void linkifyReplacesRequestConnectToUrlFailed() {
        String msg = "I/O error on POST request: Connect to https://example.com:443 failed: Connect timed out";

        assertThat(ServiceFailureNotification.linkifyRequestUrl(msg))
                .isEqualTo("I/O error on POST <https://example.com:443|request>: Connect timed out");
    }

    @Test
    void linkifyLeavesMessagesWithoutRequestUrlUntouched() {
        String msg = "Connection refused";
        assertThat(ServiceFailureNotification.linkifyRequestUrl(msg)).isEqualTo(msg);
    }

    @Test
    void linkifyHandlesNull() {
        assertThat(ServiceFailureNotification.linkifyRequestUrl(null)).isNull();
    }

    @Test
    void shortNameUsesShortNameProviderWhenAvailable() {
        ShortNameProvider provider = () -> "MQTT";
        assertThat(ServiceFailureNotification.shortName(provider)).isEqualTo("MQTT");
    }

    @Test
    void shortNameFallsBackToToStringForOtherKeys() {
        Object key = new Object() {
            @Override public String toString() { return "LOGIN"; }
        };
        assertThat(ServiceFailureNotification.shortName(key)).isEqualTo("LOGIN");
    }

}
