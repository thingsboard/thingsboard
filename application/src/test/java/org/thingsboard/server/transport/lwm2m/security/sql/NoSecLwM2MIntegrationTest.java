/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredential;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.COAP_CONFIG;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.SECURITY;

@Slf4j
public class NoSecLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testConnectAndObserveTelemetry() throws Exception {
        NoSecClientCredential clientCredentials =  createNoSecClientCredentials(CLIENT_ENDPOINT_NO_SEC);
        super.basicTestConnectionObserveTelemetry(SECURITY, clientCredentials, COAP_CONFIG, CLIENT_ENDPOINT_NO_SEC);
    }

}
