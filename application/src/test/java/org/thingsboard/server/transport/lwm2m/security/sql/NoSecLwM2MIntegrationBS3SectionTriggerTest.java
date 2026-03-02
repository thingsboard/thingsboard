/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import org.junit.Test;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.LWM2M_ONLY;
public class NoSecLwM2MIntegrationBS3SectionTriggerTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testWithNoSecConnectLwm2mSuccessBootstrapRequestTrigger_3_ConnectBsSuccess_UpdateLwm2mSection_3_AndLm2m_1_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC_BS + "Trigger_3" + LWM2M_ONLY.name();
        String awaitAlias = "await on client state (NoSecBS Trigger Lwm2m section)";
        basicTestConnectionBootstrapRequestTriggerBefore(clientEndpoint, awaitAlias, LWM2M_ONLY, 3);
    }
}
