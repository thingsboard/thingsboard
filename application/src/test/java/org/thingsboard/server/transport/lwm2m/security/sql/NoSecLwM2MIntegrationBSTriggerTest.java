// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.junit.Test;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;

public class NoSecLwM2MIntegrationBSTriggerTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTriggerConnectBsSuccess_UpdateTwoSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger" + BOTH.name();
        String awaitAlias = "await on client state (NoSecBS Trigger Two section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, BOTH, 1);
    }
}

