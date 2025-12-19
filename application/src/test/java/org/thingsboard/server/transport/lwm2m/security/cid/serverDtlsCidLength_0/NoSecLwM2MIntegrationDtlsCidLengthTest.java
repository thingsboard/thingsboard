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
package org.thingsboard.server.transport.lwm2m.security.cid.serverDtlsCidLength_0;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.transport.lwm2m.security.cid.AbstractSecurityLwM2MIntegrationDtlsCidLength0Test;

import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.NO_SEC;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class NoSecLwM2MIntegrationDtlsCidLengthTest extends AbstractSecurityLwM2MIntegrationDtlsCidLength0Test {

    @Before
    public void setUpNoSecDtlsCidLength() {
        transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(NO_SEC, NONE));
        awaitAlias = "await on client state (NoSec_Lwm2m) serverDtlsCidLength = 0";
    }

    @Test
    public void testWithNoSecConnectLwm2mSuccessClientDtlsCidLength_Null() throws Exception {
        testNoSecDtlsCidLength(null);
    }

    @Test
    public void testWithNoSecConnectLwm2mSuccessClientDtlsCidLength_0() throws Exception {
        testNoSecDtlsCidLength(0);
    }

    @Test
    public void testWithNoSecConnectLwm2mSuccessClientDtlsCidLength_1() throws Exception {
        testNoSecDtlsCidLength(1);
    }

    @Test
    public void testWithNoSecConnectLwm2mSuccessClientDtlsCidLength_2() throws Exception {
        testNoSecDtlsCidLength(1);
    }

    @Test
    public void testWithNoSecConnectLwm2mSuccessClientDtlsCidLength_4() throws Exception {
        testNoSecDtlsCidLength(4);
    }

    @Test
    public void testWithNoSecConnectLwm2mSuccessClientDtlsCidLength_16() throws Exception {
        testNoSecDtlsCidLength(16);
    }
}
