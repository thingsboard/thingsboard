// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.junit.Test;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOOTSTRAP_ONLY;
public class NoSecLwM2MIntegrationBSOnlyTriggerOneSectionTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTriggerConnectBsSuccess_UpdateBootstrapSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger" + BOOTSTRAP_ONLY.name();
        String awaitAlias = "await on client state (NoSecBS Trigger Bootstrap section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, BOOTSTRAP_ONLY, 1);
    }
}
