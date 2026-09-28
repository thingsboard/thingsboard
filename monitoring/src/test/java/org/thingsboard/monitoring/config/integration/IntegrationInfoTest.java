// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.monitoring.config.integration;

import org.junit.jupiter.api.Test;
import org.thingsboard.monitoring.data.notification.ServiceFailureNotification;
import org.thingsboard.monitoring.data.notification.ShortNameProvider;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationInfoTest {

    @Test
    void getShortNameDistinguishesFromTransportWithIPrefix() {
        IntegrationInfo info = new IntegrationInfo(IntegrationType.MQTT, "https://example.com");
        assertThat(info.getShortName()).isEqualTo("iMQTT");
    }

    @Test
    void implementsShortNameProvider() {
        IntegrationInfo info = new IntegrationInfo(IntegrationType.HTTP, "https://example.com");
        assertThat(info).isInstanceOf(ShortNameProvider.class);
    }

    @Test
    void serviceFailureNotificationUsesShortNameForIntegration() {
        IntegrationInfo info = new IntegrationInfo(IntegrationType.COAP, "coap://example.com:5683");
        ServiceFailureNotification notification = new ServiceFailureNotification(info, new RuntimeException("boom"), 2);

        assertThat(notification.getAffectedServices())
                .singleElement()
                .extracting("name")
                .isEqualTo("iCoAP");
    }

}
