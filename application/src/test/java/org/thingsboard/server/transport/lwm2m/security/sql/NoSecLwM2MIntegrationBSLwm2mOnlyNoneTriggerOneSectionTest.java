// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.junit.Test;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.LWM2M_ONLY;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;
public class NoSecLwM2MIntegrationBSLwm2mOnlyNoneTriggerOneSectionTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTriggerConnectBsSuccess_UpdateLwm2mSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger" + LWM2M_ONLY.name();
        String awaitAlias = "await on client state (NoSecBS Trigger Lwm2m section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, LWM2M_ONLY, 1);
    }

    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTriggerConnectBsSuccess_UpdateNoneSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger" + NONE.name();
        String awaitAlias = "await on client state (NoSecBS Trigger None  section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, NONE, 1);
    }
}
