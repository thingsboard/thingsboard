/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.security.sql.serverDtlsCidLength_3;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.object.Security;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.device.credentials.lwm2m.AbstractLwM2MClientSecurityCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.NO_SEC;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_INIT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_READ_CONNECTION_ID;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_WRITE_CONNECTION_ID;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;
import static org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest.SERVER_DTLS_CID_LENGTH;


@TestPropertySource(properties = {
        "transport.lwm2m.dtls.connection_id_length=" + SERVER_DTLS_CID_LENGTH
})

@DaoSqlTest
@Slf4j
public abstract class AbstractSecurityLwM2MIntegrationDtlsCidLengthTest extends AbstractSecurityLwM2MIntegrationTest {

    protected AbstractLwM2MClientSecurityCredential clientCredentials;
    protected Security security;
    protected Lwm2mDeviceProfileTransportConfiguration transportConfiguration;
    protected LwM2MDeviceCredentials deviceCredentials;
    protected String clientEndpoint;
    protected LwM2MSecurityMode lwM2MSecurityMode;
    protected String awaitAlias;
    protected final Random randomSuffix = new Random();

    protected final Set<LwM2MClientState> expectedStatusesRegistrationLwm2mDtlsCidSuccess = new HashSet<>(Arrays.asList(ON_INIT, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS, ON_READ_CONNECTION_ID, ON_WRITE_CONNECTION_ID));


    protected void testNoSecDtlsCidLength(Integer dtlsCidLength) throws Exception {
        testDtlsCidLength(dtlsCidLength, expectedStatusesRegistrationLwm2mSuccess);
    }
    protected void testPskDtlsCidLength(Integer dtlsCidLength) throws Exception {
        testDtlsCidLength(dtlsCidLength, expectedStatusesRegistrationLwm2mDtlsCidSuccess);
    }

    protected void testDtlsCidLength(Integer dtlsCidLength,  Set<LwM2MClientState> expectedStatuses) throws Exception {
        this.basicTestConnectionDtlsCidLength(
                security,
                deviceCredentials,
                COAP_CONFIG,
                clientEndpoint,
                transportConfiguration,
                awaitAlias,
                expectedStatuses,
                dtlsCidLength);
    }

    protected void initNoSecClient(String clientEndpointNext){
        lwM2MSecurityMode = NO_SEC;
        security = SECURITY_NO_SEC;
        clientEndpoint = clientEndpointNext;
        transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(lwM2MSecurityMode, NONE));
        deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        awaitAlias = "await on client state (NoSec_Lwm2m)";
    }
}
