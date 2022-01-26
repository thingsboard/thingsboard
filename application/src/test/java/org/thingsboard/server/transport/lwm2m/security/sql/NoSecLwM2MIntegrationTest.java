/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.eclipse.leshan.client.object.Security;
import org.junit.Test;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import static org.eclipse.leshan.client.object.Security.noSecBootstap;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.NO_SEC;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;

public class NoSecLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    //Lwm2m only
    @Test
    public void testWithNoSecConnectLwm2mSuccessAndObserveTelemetry() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC;
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        super.basicTestConnectionObserveTelemetry(SECURITY_NO_SEC, clientCredentials, COAP_CONFIG, clientEndpoint);
    }

    // Bootstrap + Lwm2m
    @Test
    public void testWithNoSecConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS;
        Security securityBs = noSecBootstap(URI_BS);
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsNoSec(BOTH));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        this.basicTestConnection(securityBs,
                deviceCredentials,
                COAP_CONFIG_BS,
                clientEndpoint,
                transportConfiguration,
                "await on client state (NoSecBS two section)",
                expectedStatusesRegistrationBsSuccess,
                true,
                NO_SEC);
    }
}
