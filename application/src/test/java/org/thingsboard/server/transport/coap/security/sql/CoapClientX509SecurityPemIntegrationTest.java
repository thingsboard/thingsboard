// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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