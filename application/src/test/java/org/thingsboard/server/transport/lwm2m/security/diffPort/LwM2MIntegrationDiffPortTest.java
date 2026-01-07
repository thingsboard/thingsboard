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
