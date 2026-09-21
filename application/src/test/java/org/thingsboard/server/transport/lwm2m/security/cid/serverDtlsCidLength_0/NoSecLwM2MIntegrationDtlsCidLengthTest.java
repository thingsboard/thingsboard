// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
        awaitAlias = "await on client state (NoSec_Lwm2m) DtlsCidLength = 0";
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
    public void testWithNoSecConnectLwm2mSuccessClientDtlsCidLength_2() throws Exception {
        testNoSecDtlsCidLength(2);
    }
}
