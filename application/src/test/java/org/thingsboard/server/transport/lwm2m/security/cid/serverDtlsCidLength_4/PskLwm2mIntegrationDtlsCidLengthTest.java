// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.security.cid.serverDtlsCidLength_4;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.transport.lwm2m.security.cid.AbstractSecurityLwM2MIntegrationDtlsCidLength4Test;

import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.PSK;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class PskLwm2mIntegrationDtlsCidLengthTest extends AbstractSecurityLwM2MIntegrationDtlsCidLength4Test {

    @Before
    public void createProfileRpc() {
        transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapSectionsServerCredentialsSecure(PSK, NONE));
        awaitAlias = "await on client state (Psk_Lwm2m) serverDtlsCidLength = 4";
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
    public void testWithPskConnectLwm2mSuccessClientDtlsCidLength_1() throws Exception {
        testPskDtlsCidLength(1);
    }

    @Test
    public void testWithPskConnectLwm2mSuccessClientDtlsCidLength_2() throws Exception {
        testPskDtlsCidLength(2);
    }

    @Test
    public void testWithPskConnectLwm2mSuccessClientDtlsCidLength_4() throws Exception {
        testPskDtlsCidLength(4);
    }

    @Test
    public void testWithPskConnectLwm2mSuccessClientDtlsCidLength_16() throws Exception {
        testPskDtlsCidLength(16);
    }
}

