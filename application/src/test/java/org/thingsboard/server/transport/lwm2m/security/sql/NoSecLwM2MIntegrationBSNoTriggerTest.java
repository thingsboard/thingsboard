// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.junit.Test;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.LWM2M_ONLY;

public class NoSecLwM2MIntegrationBSNoTriggerTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testWithNoSecConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "NoTrigger" + BOTH.name();
        String awaitAlias = "await on client state (NoSecBS two section)";
        basicTestConnectionStartBS(clientEndpoint, awaitAlias, BOTH, expectedStatusesRegistrationBsSuccess, ON_REGISTRATION_SUCCESS);
    }

    @Test
    public void testWithNoSecConnectBsSuccess_UpdateLwm2mSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "NoTrigger" + LWM2M_ONLY.name();
        String awaitAlias = "await on client state (NoSecBS Lwm2m section)";
        basicTestConnectionStartBS(clientEndpoint, awaitAlias, LWM2M_ONLY, expectedStatusesRegistrationBsSuccess, ON_REGISTRATION_SUCCESS);
    }
}
