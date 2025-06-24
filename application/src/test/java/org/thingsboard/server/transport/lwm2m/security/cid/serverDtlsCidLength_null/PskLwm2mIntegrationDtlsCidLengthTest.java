/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.security.cid.serverDtlsCidLength_null;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.transport.lwm2m.security.cid.AbstractSecurityLwM2MIntegrationDtlsCidLengthNullTest;

import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.PSK;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class PskLwm2mIntegrationDtlsCidLengthTest extends AbstractSecurityLwM2MIntegrationDtlsCidLengthNullTest {

    @Before
    public void createProfileRpc() {
        transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, NONE));
        awaitAlias = "await on client state (Psk_Lwm2m) DtlsCidLength = Null";
    }

    @Test
    public void testWithPskConnectLwm2mSuccessClientDtlsCidLength_Null() throws Exception {
        testPskDtlsCidLength(null);
    }

    @Test
    public void testWithPskConnectLwm2mSuccessClientDtlsCidLength_0() throws Exception {
        testPskDtlsCidLength(0);
    }

    @Test
    public void testWithPskConnectLwm2mSuccessClientDtlsCidLength_2() throws Exception {
        testPskDtlsCidLength(2);
    }
}

