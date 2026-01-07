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
package org.thingsboard.server.transport.coap.security.sql;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.security.AbstractCoapSecurityIntegrationTest;

@Slf4j
@DaoSqlTest
public class CoapClientX509SecurityPemIntegrationTest extends AbstractCoapSecurityIntegrationTest {
    @Before
    public void beforeTest() throws Exception {
        processBeforeTest();
    }

    @Test
    public void testX509NoTrustFromPathConnectCoapSuccessUpdateAttributesSuccess() throws Exception {
        clientX509FromPathUpdateFeatureTypeTest(FeatureType.ATTRIBUTES);
    }
    @Test
    public void testX509NoTrustFromPathConnectCoapSuccessUpdateTelemetrySuccess() throws Exception {
        clientX509FromPathUpdateFeatureTypeTest(FeatureType.TELEMETRY);
    }    @Test
    public void testTwoDevicesWithSamePortX509NoTrustFromPathConnectCoapSuccess() throws Exception {
        twoClientWithSamePortX509FromPathConnectTest();
    }
}