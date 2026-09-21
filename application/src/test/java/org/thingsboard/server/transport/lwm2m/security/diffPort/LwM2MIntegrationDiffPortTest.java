// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.security.diffPort;

import org.junit.Test;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;

import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.NO_SEC;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.PSK;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class LwM2MIntegrationDiffPortTest extends AbstractLwM2MIntegrationDiffPortTest {

    @Test
    public void testWithNoSecConnectLwm2mSuccess_AfterRegistration_UpdateRegistrationFromDifferentPort_Ok() throws Exception {
        String awaitAlias = "await on client state (NoSec different port)";
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(NO_SEC, NONE));
        initDeviceCredentialsNoSek();
        basicTestConnectionDifferentPort(
                transportConfiguration,
                awaitAlias);
    }
    @Test
    public void testWithPskConnectLwm2mSuccess_AfterRegistration_UpdateRegistrationFromDifferentPort_Ok() throws Exception {
        String awaitAlias = "await on client state (Psk different port)";
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, NONE));
        initDeviceCredentialsPsk();
        basicTestConnectionDifferentPort(
                transportConfiguration,
                awaitAlias);
    }
}
