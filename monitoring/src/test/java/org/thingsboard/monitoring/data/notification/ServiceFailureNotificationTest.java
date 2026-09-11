// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
