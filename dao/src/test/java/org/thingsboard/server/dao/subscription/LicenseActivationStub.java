// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.client.NonProductionTbLicenseClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Hands a {@link BasicSubscriptionService} the licence client it is to read, now that the client itself
 * belongs to {@link LicenseActivationService}. These tests never go through the container, so the activation
 * service is stubbed rather than created - and a {@code null} client is how a locked instance is expressed.
 */
final class LicenseActivationStub {

    private LicenseActivationStub() {
    }

    /** @return the stub, so a test that varies the licence version can re-stub it per case. */
    static LicenseActivationService wire(Object subscriptionService, AbstractTbLicenseClient client, int licenseVersion) {
        LicenseActivationService licenseActivationService = mock(LicenseActivationService.class);
        when(licenseActivationService.getClient()).thenReturn(client);
        when(licenseActivationService.getLicenseVersion()).thenReturn(licenseVersion);
        when(licenseActivationService.isNonProductionMode()).thenReturn(client instanceof NonProductionTbLicenseClient);
        ReflectionTestUtils.setField(subscriptionService, "licenseActivationService", licenseActivationService);
        return licenseActivationService;
    }

}
